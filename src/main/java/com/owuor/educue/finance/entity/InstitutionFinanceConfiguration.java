package com.owuor.educue.finance.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity @Table(name="institution_finance_configuration") @Getter @Setter
public class InstitutionFinanceConfiguration {
    @Id private Long id = 1L;
    @Column(name="paybill_short_code", length=20) private String paybillShortCode;
    @Column(name="account_reference_instructions", length=200) private String accountReferenceInstructions;
    @Column(name="bank_name", length=120) private String bankName;
    @Column(name="bank_account_name", length=150) private String bankAccountName;
    @Column(name="bank_account_number", length=80) private String bankAccountNumber;
    @Column(name="bank_branch", length=120) private String bankBranch;
    @Version private Long version;
    @Column(name="updated_at", nullable=false) private LocalDateTime updatedAt;
    @PrePersist @PreUpdate void touch(){updatedAt=LocalDateTime.now();}
}
