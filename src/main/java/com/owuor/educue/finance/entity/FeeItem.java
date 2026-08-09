package com.owuor.educue.finance.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "fee_items",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_fee_items_uuid",
                        columnNames = "uuid"
                ),
                @UniqueConstraint(
                        name = "uq_fee_items_code",
                        columnNames = "code"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
public class FeeItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false)
    private UUID uuid;

    /**
     * Stable code such as:
     * TUITION
     * MEDICAL
     * REGISTRATION
     * TECHNOLOGY
     */
    @Column(
            nullable = false,
            unique = true,
            updatable = false,
            length = 50
    )
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 100)
    private String category;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();

        if (uuid == null) {
            uuid = UUID.randomUUID();
        }

        if (code != null) {
            code = code.trim().toUpperCase();
        }

        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        if (code != null) {
            code = code.trim().toUpperCase();
        }

        updatedAt = LocalDateTime.now();
    }
}
