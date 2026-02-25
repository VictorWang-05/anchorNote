package com.csci310.anchornotes.repository;

import com.csci310.anchornotes.entity.Template;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TemplateRepository extends JpaRepository<Template, Long> {

    @EntityGraph(attributePaths = {"tags", "geofence", "image", "audio"})
    List<Template> findByUserId(UUID userId);

    @EntityGraph(attributePaths = {"tags", "geofence", "image", "audio"})
    Optional<Template> findByIdAndUserId(Long id, UUID userId);

    void deleteByIdAndUserId(Long id, UUID userId);
}
