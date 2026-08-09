package com.owuor.educue.institution.entity;

import com.owuor.educue.institution.enums.AcademicActivityType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "academic_activity_deadlines", uniqueConstraints =
        @UniqueConstraint(name = "uk_activity_deadline_year_type", columnNames = {"academic_year_id", "activity_type"}))
@Getter @Setter @NoArgsConstructor
public class AcademicActivityDeadline {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, unique = true, updatable = false) private UUID uuid;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "academic_year_id", nullable = false) private AcademicYear academicYear;
    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false, length = 40) private AcademicActivityType activityType;
    @Column(name = "deadline_at", nullable = false) private LocalDateTime deadlineAt;
    @Column(name = "starts_at") private LocalDateTime startsAt;
    @Column(length = 250) private String description;
    @Column(nullable = false) private boolean active = true;
    @Version @Column(nullable = false) private Long version;
    @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;

    @PrePersist void create() { if (uuid == null) uuid = UUID.randomUUID(); createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate void update() { updatedAt = LocalDateTime.now(); }
}
