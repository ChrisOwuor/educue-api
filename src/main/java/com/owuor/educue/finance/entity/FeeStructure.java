package com.owuor.educue.finance.entity;

import com.owuor.educue.academics.entity.CourseAcademicPeriod;
import com.owuor.educue.admissions.entity.IntakeCourse;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "fee_structures",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_fee_structure",
                        columnNames = {
                                "intake_course_id",
                                "course_academic_period_id"
                        }
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
public class FeeStructure {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "intake_course_id", nullable = false)
    private IntakeCourse intakeCourse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_academic_period_id", nullable = false)
    private CourseAcademicPeriod courseAcademicPeriod;

    @Column(nullable = false)
    private boolean active = true;

    @OneToMany(
            mappedBy = "feeStructure",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<FeeStructureItem> items = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;
}
