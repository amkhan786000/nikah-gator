package org.nikahgator.repository;

import java.util.List;
import java.util.Optional;

import org.nikahgator.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findByRequestIdOrderByIdDesc(Long requestId);

    Optional<Document> findByIdAndRequestId(Long id, Long requestId);

    List<Document> findByChecklistId(Long checklistId);
}
