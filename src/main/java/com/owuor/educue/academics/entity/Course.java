package com.owuor.educue.academics.entity;

import com.owuor.educue.academics.enums.QualificationType;
import com.owuor.educue.academics.enums.StudyMode;
import com.owuor.educue.institution.entity.Department;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "courses")
@Getter
@Setter
@NoArgsConstructor
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Public-safe identifier (never expose DB ID)
    @Column(nullable = false, unique = true, updatable = false)
    private UUID uuid;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false)
    private Integer durationValue;

    @Column(nullable = false, length = 10)
    private String durationUnit; // MONTHS | YEARS | WEEKS


    @Column(nullable = false)
    private boolean active = true;

    // ===== AUDIT FIELDS =====
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ===== LIFECYCLE HOOKS =====
    @PrePersist
    public void onCreate() {
        if (uuid == null) {
            uuid = UUID.randomUUID();
        }
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    //==== ADDED FIELDS===//
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QualificationType qualificationType;

    @Enumerated(EnumType.STRING)
    private StudyMode studyMode;

    @Column(name = "total_credits")
    private Integer totalCredits;

    @Version
    private Long version;

    @Column(name = "award_title", length = 150)
    private String awardTitle;


}
