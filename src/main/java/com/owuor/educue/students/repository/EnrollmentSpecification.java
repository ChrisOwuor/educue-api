package com.owuor.educue.students.repository;

import com.owuor.educue.students.entity.Enrollment;
import org.springframework.data.jpa.domain.Specification;

public class EnrollmentSpecification {

    public static Specification<Enrollment> search(String search) {
        return (root, query, cb) -> {

            if (search == null || search.isBlank()) {
                return null;
            }

            String like = "%" + search.toLowerCase() + "%";

            return cb.or(
                    cb.like(
                            cb.lower(
                                    root.get("student")
                                            .get("fullName")
                            ),
                            like
                    ),
                    cb.like(
                            cb.lower(
                                    root.get("student")
                                            .get("admissionNumber")
                            ),
                            like
                    ),
                    cb.like(
                            cb.lower(
                                    root.get("intakeCourse").get("course")
                                            .get("name")
                            ),
                            like
                    ),
                    cb.like(
                            cb.lower(
                                    root.get("intakeCourse").get("course")
                                            .get("code")
                            ),
                            like
                    )
            );
        };
    }

    public static Specification<Enrollment> status(String status) {
        return (root, query, cb) -> {

            if (status == null || status.isBlank()) {
                return null;
            }

            return cb.equal(root.get("status"), status);
        };
    }

    public static Specification<Enrollment> course(Long courseId) {
        return (root, query, cb) -> {

            if (courseId == null) {
                return null;
            }

            return cb.equal(
                    root.get("intakeCourse").get("course").get("id"),
                    courseId
            );
        };
    }

    public static Specification<Enrollment> courseAcademicPeriod(Long courseAcademicPeriodId) {

        return (root, query, cb) -> {

            if (courseAcademicPeriodId == null) {
                return null;
            }

            return cb.equal(
                    root.get("currentCourseAcademicPeriod").get("id"),
                    courseAcademicPeriodId
            );
        };
    }

}
