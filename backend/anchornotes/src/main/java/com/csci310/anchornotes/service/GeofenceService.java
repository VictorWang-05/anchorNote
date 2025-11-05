package com.csci310.anchornotes.service;

import com.csci310.anchornotes.dto.geofence.GeofenceRegistrationResponse;
import com.csci310.anchornotes.entity.Note;
import com.csci310.anchornotes.repository.NoteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeofenceService {

    private final NoteRepository noteRepository;

    /**
     * List all geofences for device registration
     * Returns all notes with geofences for this user
     */
    @Transactional(readOnly = true)
    public List<GeofenceRegistrationResponse> listGeofencesForRegistration(String userId) {
        log.info("Fetching geofences for registration for user: {}", userId);

        // Get all notes with geofences for this user
        UUID userUuid = UUID.fromString(userId);
        List<Note> notesWithGeofences = noteRepository.findByUserIdAndGeofenceIsNotNull(userUuid);

        List<GeofenceRegistrationResponse> responses = notesWithGeofences.stream()
                .filter(note -> note.getGeofence() != null)
                .map(note -> GeofenceRegistrationResponse.builder()
                        .geofenceId("note_" + note.getId())
                        .latitude(note.getGeofence().getLatitude())
                        .longitude(note.getGeofence().getLongitude())
                        .radiusMeters(note.getGeofence().getRadius())
                        .addressName(note.getGeofence().getAddressName())
                        .build())
                .collect(Collectors.toList());

        log.info("Found {} geofences for user: {}", responses.size(), userId);

        return responses;
    }
}
