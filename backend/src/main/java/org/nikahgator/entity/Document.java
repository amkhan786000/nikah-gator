package org.nikahgator.entity;

import org.nikahgator.audit.Auditable;
import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "documents")
@Getter @Setter
public class Document extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_id")
    private Long requestId;

    @Column(name = "checklist_id")
    private Long checklistId;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "stored_path")
    private String storedPath;

    @Column(name = "content_type")
    private String contentType;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Column(name = "uploaded_by")
    private Long uploadedBy;

}
