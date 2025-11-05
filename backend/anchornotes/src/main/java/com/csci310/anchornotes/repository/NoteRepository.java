package com.csci310.anchornotes.repository;

import com.csci310.anchornotes.entity.Note;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NoteRepository extends JpaRepository<Note, Long> {

    // Find by user
    List<Note> findByUserIdOrderByLastEditedDesc(UUID userId, Pageable pageable);

    // Find all notes by user with full entity graph
    @EntityGraph(attributePaths = {"tags", "geofence", "image", "audio"})
    List<Note> findAllByUserIdOrderByLastEditedDesc(UUID userId);

    @EntityGraph(attributePaths = {"tags", "geofence", "image", "audio"})
    Optional<Note> findByIdAndUserId(Long id, UUID userId);

    // Find notes with geofences
    @EntityGraph(attributePaths = {"geofence"})
    List<Note> findByUserIdAndGeofenceIsNotNull(UUID userId);

    // For Relevant Notes - Time-based (within time window)
    @Query("SELECT n FROM Note n WHERE n.userId = :userId " +
           "AND n.reminderTime BETWEEN :startTime AND :endTime " +
           "ORDER BY n.lastEdited DESC")
    List<Note> findTimeRelevantNotes(
        @Param("userId") UUID userId,
        @Param("startTime") Instant startTime,
        @Param("endTime") Instant endTime
    );

    // For Relevant Notes - Geofence-based
    @Query("SELECT n FROM Note n WHERE n.userId = :userId " +
           "AND n.id IN :noteIds")
    List<Note> findGeofenceRelevantNotes(
        @Param("userId") UUID userId,
        @Param("noteIds") List<Long> noteIds
    );

    // Simple search - by title, content, and geofence address name
    @Query(value = "SELECT DISTINCT n.* FROM notes n " +
           "LEFT JOIN geofence g ON n.geofence = g.id " +
           "WHERE n.user_id = :userId " +
           "AND (:query = '' OR LOWER(n.text) LIKE LOWER('%' || :query || '%') " +
           "     OR LOWER(n.title) LIKE LOWER('%' || :query || '%') " +
           "     OR LOWER(g.address_name) LIKE LOWER('%' || :query || '%')) " +
           "ORDER BY n.last_edited DESC " +
           "/*#pageable*/",
           countQuery = "SELECT COUNT(DISTINCT n.id) FROM notes n " +
                       "LEFT JOIN geofence g ON n.geofence = g.id " +
                       "WHERE n.user_id = :userId " +
                       "AND (:query = '' OR LOWER(n.text) LIKE LOWER('%' || :query || '%') " +
                       "     OR LOWER(n.title) LIKE LOWER('%' || :query || '%') " +
                       "     OR LOWER(g.address_name) LIKE LOWER('%' || :query || '%'))",
           nativeQuery = true)
    Page<Note> searchNotes(
        @Param("userId") UUID userId,
        @Param("query") String query,
        Pageable pageable
    );

    // Filter notes - apply filters to a set of note IDs
    @Query(value = "SELECT DISTINCT n.* FROM notes n LEFT JOIN note_tags nt ON n.id = nt.note_id " +
           "WHERE n.user_id = :userId " +
           "AND (CAST(:hasTagFilter AS BOOLEAN) = false OR nt.tag_id = ANY(CAST(:tagIds AS BIGINT[]))) " +
           "AND (:hasPhoto IS NULL OR (:hasPhoto = true AND n.image_file IS NOT NULL) OR (:hasPhoto = false AND n.image_file IS NULL)) " +
           "AND (:hasAudio IS NULL OR (:hasAudio = true AND n.audio_file IS NOT NULL) OR (:hasAudio = false AND n.audio_file IS NULL)) " +
           "AND (:hasLocation IS NULL OR (:hasLocation = true AND n.geofence IS NOT NULL) OR (:hasLocation = false AND n.geofence IS NULL)) " +
           "AND (CAST(:editedStart AS TIMESTAMP) IS NULL OR n.last_edited >= CAST(:editedStart AS TIMESTAMP)) " +
           "AND (CAST(:editedEnd AS TIMESTAMP) IS NULL OR n.last_edited <= CAST(:editedEnd AS TIMESTAMP)) " +
           "ORDER BY n.last_edited DESC " +
           "/*#pageable*/",
           countQuery = "SELECT COUNT(DISTINCT n.id) FROM notes n LEFT JOIN note_tags nt ON n.id = nt.note_id " +
                       "WHERE n.user_id = :userId " +
                       "AND (CAST(:hasTagFilter AS BOOLEAN) = false OR nt.tag_id = ANY(CAST(:tagIds AS BIGINT[]))) " +
                       "AND (:hasPhoto IS NULL OR (:hasPhoto = true AND n.image_file IS NOT NULL) OR (:hasPhoto = false AND n.image_file IS NULL)) " +
                       "AND (:hasAudio IS NULL OR (:hasAudio = true AND n.audio_file IS NOT NULL) OR (:hasAudio = false AND n.audio_file IS NULL)) " +
                       "AND (:hasLocation IS NULL OR (:hasLocation = true AND n.geofence IS NOT NULL) OR (:hasLocation = false AND n.geofence IS NULL)) " +
                       "AND (CAST(:editedStart AS TIMESTAMP) IS NULL OR n.last_edited >= CAST(:editedStart AS TIMESTAMP)) " +
                       "AND (CAST(:editedEnd AS TIMESTAMP) IS NULL OR n.last_edited <= CAST(:editedEnd AS TIMESTAMP))",
           nativeQuery = true)
    Page<Note> filterNotes(
        @Param("userId") UUID userId,
        @Param("hasTagFilter") boolean hasTagFilter,
        @Param("tagIds") String tagIds,
        @Param("hasPhoto") Boolean hasPhoto,
        @Param("hasAudio") Boolean hasAudio,
        @Param("hasLocation") Boolean hasLocation,
        @Param("editedStart") Instant editedStart,
        @Param("editedEnd") Instant editedEnd,
        Pageable pageable
    );

    void deleteByIdAndUserId(Long id, UUID userId);
}
