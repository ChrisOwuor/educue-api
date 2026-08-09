package com.owuor.educue.admissions.entity;

import com.owuor.educue.admissions.enums.IntakeStatus;
import com.owuor.educue.institution.entity.AcademicYear;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.*;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;


@Entity
@Table(name = "intakes")
@Getter
@Setter
@NoArgsConstructor
public class Intake {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false)
    private UUID uuid;

    @Column(nullable = false, unique = true, length = 100)
    private String name; // e.g. "September 2026"

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "application_deadline", nullable = false)
    private LocalDate applicationDeadline;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IntakeStatus status = IntakeStatus.UPCOMING;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "academic_year_id", nullable = false)
    private AcademicYear academicYear;

    @Generated(
            event = EventType.INSERT,
            sql = "nextval('intake_sequence_number_seq')"
    )
    @Column(
            name = "sequence_number",
            nullable = false,
            unique = true,
            updatable = false
    )
    private Long sequenceNumber;

}
