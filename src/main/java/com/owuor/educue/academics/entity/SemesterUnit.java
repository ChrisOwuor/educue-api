package com.owuor.educue.academics.entity;

import com.owuor.educue.academics.enums.SemesterUnitCategory;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "semester_units")
@Getter
@Setter
@NoArgsConstructor
public class SemesterUnit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "semester_id", nullable = false)
    private Semester semester;

    // Deliberately NOT restricted to the semester's course's department -
    // a curriculum is free to reference any Unit regardless of which
    // department owns it (e.g. Business's curriculum borrowing ICT's
    // "Communication Skills" unit). The owning department still retains
    // exclusive trainer-assignment rights for that unit.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_id", nullable = false)
    private Unit unit;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SemesterUnitCategory category;

    // The field promotion logic actually reads - "must this be passed to
    // progress" - kept distinct from `category`, since COMMON units may
    // or may not be mandatory depending on institution policy.
    @Column(name = "is_mandatory", nullable = false)
    private boolean mandatory = true;
}
