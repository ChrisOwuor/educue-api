package com.owuor.educue.graduation.entity;

import com.owuor.educue.graduation.enums.GraduationBatchStatus;
import com.owuor.educue.institution.entity.AcademicYear;
import com.owuor.educue.users.entity.User;
import jakarta.persistence.*;
import lombok.Getter; import lombok.NoArgsConstructor; import lombok.Setter;
import java.time.*; import java.util.*;

@Entity @Table(name="graduation_batches") @Getter @Setter @NoArgsConstructor
public class GraduationBatch {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false,unique=true,updatable=false) private UUID uuid=UUID.randomUUID();
 @Column(nullable=false,length=150) private String name;
 @Column(name="graduation_date",nullable=false) private LocalDate graduationDate;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="academic_year_id",nullable=false) private AcademicYear academicYear;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=40) private GraduationBatchStatus status=GraduationBatchStatus.DRAFT;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="created_by",nullable=false) private User createdBy;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="conferred_by") private User conferredBy;
 @Column(name="conferred_at") private LocalDateTime conferredAt;
 @Column(name="total_candidates",nullable=false) private int totalCandidates;
 @Column(name="processed_candidates",nullable=false) private int processedCandidates;
 @Column(name="successful_candidates",nullable=false) private int successfulCandidates;
 @Column(name="failed_candidates",nullable=false) private int failedCandidates;
 @Column(name="created_at",nullable=false,updatable=false) private LocalDateTime createdAt=LocalDateTime.now();
 @Column(name="updated_at",nullable=false) private LocalDateTime updatedAt=LocalDateTime.now();
 @Version private Long version;
 @PreUpdate void updateTime(){updatedAt=LocalDateTime.now();}
}
