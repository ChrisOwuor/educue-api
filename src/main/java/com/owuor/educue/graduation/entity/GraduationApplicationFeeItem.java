package com.owuor.educue.graduation.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "graduation_application_fee_items")
@Getter @Setter @NoArgsConstructor
public class GraduationApplicationFeeItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "application_id", nullable = false, updatable = false) private GraduationApplication application;
    @Column(name = "fee_item_uuid", nullable = false, updatable = false) private UUID feeItemUuid;
    @Column(name = "fee_item_code", nullable = false, length = 50, updatable = false) private String feeItemCode;
    @Column(name = "fee_item_name", nullable = false, length = 150, updatable = false) private String feeItemName;
    @Column(nullable = false, precision = 15, scale = 2, updatable = false) private BigDecimal amount;
    @Column(name = "display_order", nullable = false, updatable = false) private int displayOrder;
}
