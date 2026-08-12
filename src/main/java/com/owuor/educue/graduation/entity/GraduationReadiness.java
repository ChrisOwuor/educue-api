package com.owuor.educue.graduation.entity;

import com.owuor.educue.users.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "graduation_readiness", uniqueConstraints =
        @UniqueConstraint(name = "uq_graduation_readiness_candidate", columnNames = "graduation_candidate_id"))
@Getter
@Setter
@NoArgsConstructor
public class GraduationReadiness {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "graduation_candidate_id", nullable = false, updatable = false)
    private GraduationCandidate candidate;

    @Column(name = "required_units", nullable = false)
    private Integer requiredUnits;
    @Column(name = "passed_units", nullable = false)
    private Integer passedUnits;
    @Column(name = "failed_units", nullable = false)
    private Integer failedUnits;
    @Column(name = "missing_results", nullable = false)
    private Integer missingResults;
    @Column(name = "missing_units", nullable = false)
    private Integer missingUnits;
    @Column(name = "required_credits")
    private Integer requiredCredits;
    @Column(name = "earned_credits", nullable = false)
    private Integer earnedCredits;
    @Column(name = "eligible", nullable = false)
    private boolean eligible;
    @Column(name = "assessed_at", nullable = false)
    private LocalDateTime assessedAt;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assessed_by")
    private User assessedBy;
}
