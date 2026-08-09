package com.owuor.educue.clearance.entity;

import com.owuor.educue.institution.entity.*;
import com.owuor.educue.clearance.enums.ClearanceDepartmentStage;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "academic_year_clearance_departments", uniqueConstraints = @UniqueConstraint(name = "uq_year_clearance_department", columnNames = {"academic_year_id", "department_id"}))
@Getter
@Setter
@NoArgsConstructor
public class AcademicYearClearanceDepartment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "academic_year_id", nullable = false)
    private AcademicYear academicYear;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ClearanceDepartmentStage stage = ClearanceDepartmentStage.GENERAL;
    @Column(nullable = false)
    private boolean mandatory = true;
}
