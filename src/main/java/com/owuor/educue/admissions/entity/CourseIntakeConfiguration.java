package com.owuor.educue.admissions.entity;

import com.owuor.educue.academics.entity.Course;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "course_intake_configurations", uniqueConstraints = @UniqueConstraint(
        name = "uk_course_intake_configuration", columnNames = {"intake_id", "course_id"}))
@Getter @Setter @NoArgsConstructor
public class CourseIntakeConfiguration {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "intake_id", nullable = false)
    private Intake intake;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;
    @Column(name = "academic_confirmed", nullable = false)
    private boolean academicConfirmed;
    @Column(name = "fee_confirmed", nullable = false)
    private boolean feeConfirmed;
    @Column(name = "academic_confirmed_at")
    private LocalDateTime academicConfirmedAt;
    @Column(name = "fee_confirmed_at")
    private LocalDateTime feeConfirmedAt;
    @Version
    private Long version;
}
