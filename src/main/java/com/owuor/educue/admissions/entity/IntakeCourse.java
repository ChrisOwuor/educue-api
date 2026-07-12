package com.owuor.educue.admissions.entity;

import com.owuor.educue.academics.entity.Course;
import com.owuor.educue.academics.entity.CourseCurriculum;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "intake_courses")
@Getter
@Setter
@NoArgsConstructor
public class IntakeCourse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "intake_id", nullable = false)
    private Intake intake;

    // Not every Course is open in every Intake - e.g. a brand-new course
    // might only launch starting next intake. This is what the public
    // application form reads from to populate its course dropdown.

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;


}
