package com.owuor.educue.clearance.entity;

import com.owuor.educue.clearance.enums.ClearanceApplicationStatus;
import com.owuor.educue.students.entity.Enrollment;
import com.owuor.educue.institution.entity.AcademicYear;
import jakarta.persistence.*;
import lombok.*;

import java.time.*;
import java.util.*;

@Entity
@Table(name = "clearance_applications", uniqueConstraints = @UniqueConstraint(name = "uq_clearance_application_enrollment_year", columnNames = {"enrollment_id", "academic_year_id"}))
@Getter
@Setter
@NoArgsConstructor
public class ClearanceApplication {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, updatable = false)
    private UUID uuid = UUID.randomUUID();
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "enrollment_id", nullable = false, updatable = false)
    private Enrollment enrollment;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "academic_year_id", nullable = false, updatable = false)
    private AcademicYear academicYear;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ClearanceApplicationStatus status = ClearanceApplicationStatus.SUBMITTED;
    @Column(name = "applied_at", nullable = false, updatable = false)
    private LocalDateTime appliedAt = LocalDateTime.now();
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    @OneToMany(mappedBy = "application", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder asc, id asc")
    private List<ClearanceCheck> checks = new ArrayList<>();
    @Version
    private Long version;

    public void addCheck(ClearanceCheck check) {
        check.setApplication(this);
        checks.add(check);
        status = ClearanceApplicationStatus.IN_PROGRESS;
        completedAt = null;
    }
}
