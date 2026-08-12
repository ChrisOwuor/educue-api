package com.owuor.educue.graduation.entity;

import com.owuor.educue.institution.entity.*;
import com.owuor.educue.users.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.*;

@Entity
@Table(name = "graduation_lists", uniqueConstraints = @UniqueConstraint(name = "uq_graduation_list_year_department", columnNames = {"academic_year_id", "department_id"}))
@Getter
@Setter
@NoArgsConstructor
public class GraduationList {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, updatable = false)
    private java.util.UUID uuid = java.util.UUID.randomUUID();
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "academic_year_id", nullable = false)
    private AcademicYear academicYear;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;
    @Column(nullable = false, length = 30)
    private String status = "DRAFT";
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "published_by")
    private User publishedBy;
    @Column(name = "published_at")
    private LocalDateTime publishedAt;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submitted_by")
    private User submittedBy;
    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;
    @Column(name = "graduation_date")
    private LocalDate graduationDate;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "finalized_by")
    private User finalizedBy;
    @Column(name = "finalized_at")
    private LocalDateTime finalizedAt;
    @Version
    private Long version;
}
