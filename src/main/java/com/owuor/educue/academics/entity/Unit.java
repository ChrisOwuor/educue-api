package com.owuor.educue.academics.entity;

import com.owuor.educue.institution.entity.Department;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "units")
@Getter
@Setter
@NoArgsConstructor
public class Unit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Mandatory and intentional: a unit's department owns trainer-assignment
    // rights for it, regardless of which other department's course uses the
    // unit through a CourseUnitPlacement. Ownership = staffing responsibility,
    // not usage restriction.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @Column(nullable = false, unique = true, length = 30)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "credit_hours")
    private Integer creditHours;

    @Column(length = 255)
    private String description;

    @Column(nullable = false)
    private boolean active = true;

    // ===== NEWLY ADDED FIELDS =====

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Public-safe identifier used in API responses instead of exposing id.
    @Column(nullable = false, unique = true, updatable = false)
    private UUID uuid;

    // Prevents concurrent updates from silently overwriting each other.
    @Version
    @Column(nullable = false)
    private Long version;

    // Records the last time this unit was modified.
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ===== END NEWLY ADDED FIELDS =====

    @PrePersist
    public void onCreate() {
        if (uuid == null) {
            uuid = UUID.randomUUID();
        }

        LocalDateTime now = LocalDateTime.now();

        if (createdAt == null) {
            createdAt = now;
        }

        updatedAt = now;
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
