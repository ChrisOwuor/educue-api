package com.owuor.educue.finance.claims;

import com.owuor.educue.users.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity @Table(name="debit_claim_batches", indexes={@Index(name="idx_debit_claim_status",columnList="status,created_at")})
@Getter @Setter
public class DebitClaimBatch {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false,unique=true,updatable=false) private UUID uuid=UUID.randomUUID();
 @Column(nullable=false,length=160) private String title;
 @Column(nullable=false,length=30) private String category;
 @Column(nullable=false,length=255) private String reason;
 @Column(name="source_url",nullable=false,length=1000) private String sourceUrl;
 @Column(name="result_url",length=1000) private String resultUrl;
 @Column(nullable=false,length=25) private String status="DRAFT";
 @Column(name="total_rows",nullable=false) private int totalRows;
 @Column(name="processed_rows",nullable=false) private int processedRows;
 @Column(name="successful_rows",nullable=false) private int successfulRows;
 @Column(name="failed_rows",nullable=false) private int failedRows;
 @Column(name="total_amount",nullable=false,precision=14,scale=2) private BigDecimal totalAmount=BigDecimal.ZERO;
 @Column(name="last_error",length=1000) private String lastError;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="created_by",nullable=false) private User createdBy;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="confirmed_by") private User confirmedBy;
 @Column(name="created_at",nullable=false,updatable=false) private LocalDateTime createdAt=LocalDateTime.now();
 @Column(name="confirmed_at") private LocalDateTime confirmedAt;
 @Column(name="completed_at") private LocalDateTime completedAt;
 @Version private Long version;
}
