package com.owuor.educue.academics.entity;

import com.owuor.educue.users.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "trainer_assignments",
        indexes = {
                @Index(name = "idx_trainer_assignment_trainer", columnList = "trainer_id"),
                @Index(name = "idx_trainer_assignment_semester_unit", columnList = "semester_unit_id"),
                @Index(name = "idx_trainer_assignment_effective_to", columnList = "effective_to")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class TrainerAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trainer_id", nullable = false)
    private User trainer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "semester_unit_id", nullable = false)
    private SemesterUnit semesterUnit;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom = LocalDate.now();

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(name = "assigned_at", nullable = false, updatable = false)
    private LocalDateTime assignedAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_by")
    private User assignedBy;

    public boolean isActive() {
        return effectiveTo == null || effectiveTo.isAfter(LocalDate.now());
    }

    public void endAssignment(LocalDate endDate) {
        this.effectiveTo = endDate;
    }
}
