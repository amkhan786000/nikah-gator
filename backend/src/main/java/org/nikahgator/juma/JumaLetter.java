package org.nikahgator.juma;

import org.nikahgator.audit.Auditable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** A letter asking a school to excuse students for Friday (Juma) prayer. */
@Entity
@Table(name = "juma_letters")
@Getter @Setter
public class JumaLetter extends Auditable {
    public static final String ACTIVE = "ACTIVE";
    public static final String VOID = "VOID";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "masjid_id")
    private Long masjidId;

    @Column(name = "school_name")
    private String schoolName;

    @Column(name = "school_address")
    private String schoolAddress;

    @Column(name = "school_contact")
    private String schoolContact;

    private String addressee;

    @Column(name = "letter_date")
    private LocalDate letterDate;

    private boolean standing;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "window_start")
    private LocalTime windowStart;

    @Column(name = "window_end")
    private LocalTime windowEnd;

    private String note;

    private String status = ACTIVE;

    @Column(name = "void_reason")
    private String voidReason;

    @Column(name = "voided_at")
    private LocalDateTime voidedAt;

    @Column(name = "voided_by")
    private Long voidedBy;


    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "juma_letter_students", joinColumns = @JoinColumn(name = "letter_id"))
    @OrderColumn(name = "position")
    private List<Student> students = new ArrayList<>();

    @Embeddable
    @Getter @Setter
    public static class Student {
        private String name;
        private String grade;

        public Student() {
        }

        public Student(String name, String grade) {
            this.name = name;
            this.grade = grade;
        }
    }
}
