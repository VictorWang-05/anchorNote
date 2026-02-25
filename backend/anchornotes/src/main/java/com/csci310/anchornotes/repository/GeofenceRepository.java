package com.csci310.anchornotes.repository;

import com.csci310.anchornotes.entity.Geofence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GeofenceRepository extends JpaRepository<Geofence, Long> {

    @Query("SELECT DISTINCT g FROM Geofence g JOIN g.notes n WHERE n.userId = :userId")
    List<Geofence> findAllWithNotesByUserId(@Param("userId") UUID userId);

    Optional<Geofence> findByIdAndUserId(Long id, UUID userId);

    List<Geofence> findByUserId(UUID userId);
}
