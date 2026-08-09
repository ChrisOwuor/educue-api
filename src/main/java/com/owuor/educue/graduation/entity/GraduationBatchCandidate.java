package com.owuor.educue.graduation.entity;

import com.owuor.educue.graduation.enums.GraduationBatchCandidateStatus;
import jakarta.persistence.*;
import lombok.Getter; import lombok.NoArgsConstructor; import lombok.Setter;
import java.time.LocalDateTime;

@Entity @Table(name="graduation_batch_candidates",uniqueConstraints=@UniqueConstraint(name="uq_graduation_batch_candidate",columnNames={"graduation_batch_id","graduation_application_id"}))
@Getter @Setter @NoArgsConstructor
public class GraduationBatchCandidate {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="graduation_batch_id",nullable=false) private GraduationBatch batch;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="graduation_application_id",nullable=false) private GraduationApplication application;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=30) private GraduationBatchCandidateStatus status=GraduationBatchCandidateStatus.PENDING;
 @Column(name="failure_reason",length=500) private String failureReason;
 @Column(name="processed_at") private LocalDateTime processedAt;
 @Version private Long version;
}
