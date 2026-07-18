package com.owuor.educue.institution.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/** Shared identity and contact details for this EduCue installation. */
@Entity
@Table(name = "institution_profiles")
@Getter
@Setter
@NoArgsConstructor
public class InstitutionProfile {

    @Id
    private Long id;

    @Column(nullable = false, unique = true, updatable = false)
    private UUID uuid;

    @Column(nullable = false, length = 180)
    private String name;

    @Column(name = "short_name", length = 30)
    private String shortName;

    @Column(name = "registration_number", length = 80)
    private String registrationNumber;

    @Column(length = 180)
    private String motto;

    @Column(name = "official_email", length = 150)
    private String officialEmail;

    @Column(length = 30)
    private String phone;

    @Column(length = 255)
    private String website;

    @Column(length = 300)
    private String address;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        if (uuid == null) uuid = UUID.randomUUID();
        if (createdAt == null) createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
