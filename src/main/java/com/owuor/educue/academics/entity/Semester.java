package com.owuor.educue.academics.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "semesters")
@Getter
@Setter
@NoArgsConstructor
public class Semester {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Each CourseCurriculum has its OWN full set of Semester rows - never
    // shared across curricula, even when "Year 1 Semester 1" looks
    // identical on paper. This is what lets one curriculum change without
    // touching another.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_curriculum_id", nullable = false)
    private CourseCurriculum courseCurriculum;

    @Column(name = "year_number", nullable = false)
    private Integer yearNumber;

    @Column(name = "semester_number", nullable = false)
    private Integer semesterNumber;

    @Column(length = 50)
    private String name; // e.g. "Year 1 Semester 1"


//    TODO(SQL)
    // --------------------------------------------------
    // Next semester within the same curriculum
    // --------------------------------------------------
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "next_semester_id")
    private Semester nextSemester;
}
