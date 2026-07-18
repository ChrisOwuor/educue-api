package com.owuor.educue.academics.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;


@Entity
@Table(name = "course_academic_periods", uniqueConstraints = {
        @UniqueConstraint(name = "uk_course_academic_period", columnNames = { "course_id", "academic_period_id" }),
        @UniqueConstraint(name = "uk_course_period_position", columnNames = { "course_id", "position" }),
        @UniqueConstraint(name = "uk_course_period_next", columnNames = { "next_period_id" })
}, indexes = {
        @Index(name = "idx_course_period_course", columnList = "course_id"),
        @Index(name = "idx_course_period_next", columnList = "next_period_id")
})
@Getter
@Setter
@NoArgsConstructor
public class CourseAcademicPeriod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false)
    private UUID uuid;

    /*
     * The course whose progression structure this belongs to.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false, updatable = false)
    private Course course;

    /*
     * Global stage definition such as Y1S1 or Y2S2.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "academic_period_id", nullable = false, updatable = false)
    private AcademicPeriod academicPeriod;

    /*
     * Used for display, validation and finding the first stage.
     */
    @Column(nullable = false, updatable = false)
    private Integer position;

    /*
     * Direct progression destination.
     * NULL means this is the final stage.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "next_period_id", unique = true)
    private CourseAcademicPeriod nextPeriod;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        if (uuid == null) {
            uuid = UUID.randomUUID();
        }

        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
