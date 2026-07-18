package com.owuor.educue.students.entity;

import com.owuor.educue.academics.entity.CourseAcademicPeriod;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "exam_cards", uniqueConstraints =
        @UniqueConstraint(name = "uk_exam_card_student_period", columnNames = {"student_id", "course_academic_period_id"}))
@Getter @Setter @NoArgsConstructor
public class ExamCard {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "verification_code", nullable = false, unique = true, updatable = false)
    private UUID verificationCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false, updatable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_academic_period_id", nullable = false, updatable = false)
    private CourseAcademicPeriod courseAcademicPeriod;

    @Column(name = "issued_at", nullable = false, updatable = false)
    private LocalDateTime issuedAt;

    @PrePersist
    void create() {
        if (verificationCode == null) verificationCode = UUID.randomUUID();
        if (issuedAt == null) issuedAt = LocalDateTime.now();
    }
}
