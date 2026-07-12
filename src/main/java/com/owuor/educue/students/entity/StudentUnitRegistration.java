package com.owuor.educue.students.entity;

import com.owuor.educue.academics.entity.SemesterUnit;
import com.owuor.educue.academics.enums.AttemptType;
import com.owuor.educue.academics.enums.RegistrationStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "student_unit_registrations")
@Getter
@Setter
@NoArgsConstructor
public class StudentUnitRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enrollment_id", nullable = false)
    private com.owuor.educue.students.entity.Enrollment enrollment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "semester_unit_id", nullable = false)
    private SemesterUnit semesterUnit;

    @Enumerated(EnumType.STRING)
    @Column(name = "attempt_type", nullable = false, length = 20)
    private AttemptType attemptType = AttemptType.NORMAL;

    // The fix: without this, nothing distinguishes "student dropped this
    // elective two weeks in" from "student is still actively registered."
    // Also what makes proper duplicate-prevention possible - the DB
    // constraint should be "at most one ACTIVE registration per
    // (enrollment_id, semester_unit_id)", enforced at the migration level,
    // not just here.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RegistrationStatus status = RegistrationStatus.ACTIVE;

    @Column(name = "registered_at", nullable = false, updatable = false)
    private LocalDateTime registeredAt = LocalDateTime.now();
}
