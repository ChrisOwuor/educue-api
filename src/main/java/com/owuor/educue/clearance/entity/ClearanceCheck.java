package com.owuor.educue.clearance.entity;

import com.owuor.educue.clearance.enums.ClearanceCheckStatus;
import com.owuor.educue.clearance.enums.ClearanceDepartmentStage;
import com.owuor.educue.institution.entity.Department;
import com.owuor.educue.users.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "clearance_checks", uniqueConstraints = @UniqueConstraint(name = "uq_clearance_check_application_department", columnNames = {"application_id", "department_id"}))
@Getter
@Setter
@NoArgsConstructor
public class ClearanceCheck {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false, updatable = false)
    private ClearanceApplication application;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false, updatable = false)
    private Department department;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ClearanceCheckStatus status = ClearanceCheckStatus.PENDING;
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ClearanceDepartmentStage stage = ClearanceDepartmentStage.GENERAL;
    @Column(nullable = false)
    private boolean mandatory = true;
    @Column(length = 1000)
    private String remarks;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;
    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;
    @Version
    private Long version;
}
