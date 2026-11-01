package org.nikahgator.entity;

import org.nikahgator.audit.Auditable;
import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "checklist_items")
@Getter @Setter
public class ChecklistItem extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_id")
    private Long requestId;

    private String code;
    private String label;
    private boolean required = true;
    private boolean done;

    @Column(name = "done_at")
    private LocalDateTime doneAt;

    @Column(name = "sort_order")
    private int sortOrder;

    private boolean custom;
}
