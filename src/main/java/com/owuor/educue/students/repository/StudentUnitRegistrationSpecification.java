package com.owuor.educue.students.repository;

import com.owuor.educue.academics.enums.AttemptType;
import com.owuor.educue.academics.enums.RegistrationStatus;
import com.owuor.educue.students.entity.StudentUnitRegistration;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

public class StudentUnitRegistrationSpecification {

    private StudentUnitRegistrationSpecification() {
    }

    public static Specification<StudentUnitRegistration> withFilters(

            String search,

            Long courseId,

            Long curriculumId,

            Long semesterId,

            Long semesterUnitId,

            AttemptType attemptType,

            RegistrationStatus status

    ) {

        return Specification.where(search(search))
                .and(course(courseId))
                .and(curriculum(curriculumId))
                .and(semester(semesterId))
                .and(semesterUnit(semesterUnitId))
                .and(attemptType(attemptType))
                .and(status(status));

    }

    private static Specification<StudentUnitRegistration> search(String search) {

        return (root, query, cb) -> {

            if (search == null || search.isBlank()) {
                return cb.conjunction();
            }

            String value = "%" + search.toLowerCase() + "%";

            var enrollment =
                    root.join("enrollment", JoinType.INNER);

            var student =
                    enrollment.join("student", JoinType.INNER);

            var semesterUnit =
                    root.join("semesterUnit", JoinType.INNER);

            var unit =
                    semesterUnit.join("unit", JoinType.INNER);

            return cb.or(

                    cb.like(
                            cb.lower(student.get("fullName")),
                            value
                    ),

                    cb.like(
                            cb.lower(student.get("admissionNumber")),
                            value
                    ),

                    cb.like(
                            cb.lower(unit.get("code")),
                            value
                    ),

                    cb.like(
                            cb.lower(unit.get("name")),
                            value
                    )
            );
        };
    }

    private static Specification<StudentUnitRegistration> course(Long id) {

        return (root, query, cb) -> {

            if (id == null) {
                return cb.conjunction();
            }

            return cb.equal(

                    root.join("semesterUnit")
                            .join("semester")
                            .join("courseCurriculum")
                            .join("course")
                            .get("id"),

                    id
            );
        };
    }

    private static Specification<StudentUnitRegistration> curriculum(Long id) {

        return (root, query, cb) -> {

            if (id == null) {
                return cb.conjunction();
            }

            return cb.equal(

                    root.join("semesterUnit")
                            .join("semester")
                            .join("courseCurriculum")
                            .get("id"),

                    id
            );
        };
    }

    private static Specification<StudentUnitRegistration> semester(Long id) {

        return (root, query, cb) -> {

            if (id == null) {
                return cb.conjunction();
            }

            return cb.equal(

                    root.join("semesterUnit")
                            .join("semester")
                            .get("id"),

                    id
            );
        };
    }

    private static Specification<StudentUnitRegistration> semesterUnit(Long id) {

        return (root, query, cb) -> {

            if (id == null) {
                return cb.conjunction();
            }

            return cb.equal(

                    root.join("semesterUnit")
                            .get("id"),

                    id
            );
        };
    }

    private static Specification<StudentUnitRegistration> attemptType(
            AttemptType attemptType
    ) {

        return (root, query, cb) -> {

            if (attemptType == null) {
                return cb.conjunction();
            }

            return cb.equal(

                    root.get("attemptType"),

                    attemptType
            );
        };
    }

    private static Specification<StudentUnitRegistration> status(
            RegistrationStatus status
    ) {

        return (root, query, cb) -> {

            if (status == null) {
                return cb.conjunction();
            }

            return cb.equal(

                    root.get("status"),

                    status
            );
        };
    }

}
