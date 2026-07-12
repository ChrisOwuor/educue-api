package com.owuor.educue.results.repository;

import com.owuor.educue.results.entity.StudentResult;
import com.owuor.educue.results.enums.ResultStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class StudentResultSpecification {

    public static Specification<StudentResult> withFilters(
            String search,
            Long courseId,
            Long curriculumId,
            Long semesterId,
            Long semesterUnitId,
            String status,
            Boolean passed
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            var registration = root.join("studentUnitRegistration");
            var enrollment = registration.join("enrollment");
            var student = enrollment.join("student");
            var semesterUnit = registration.join("semesterUnit");
            var curriculum = enrollment.join("courseCurriculum");

            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(student.get("fullName")), pattern),
                        cb.like(cb.lower(student.get("admissionNumber")), pattern),
                        cb.like(cb.lower(semesterUnit.get("unit").get("code")), pattern),
                        cb.like(cb.lower(semesterUnit.get("unit").get("name")), pattern)
                ));
            }

            if (courseId != null) {
                predicates.add(cb.equal(curriculum.get("course").get("id"), courseId));
            }

            if (curriculumId != null) {
                predicates.add(cb.equal(curriculum.get("id"), curriculumId));
            }

            if (semesterId != null) {
                predicates.add(cb.equal(semesterUnit.get("semester").get("id"), semesterId));
            }

            if (semesterUnitId != null) {
                predicates.add(cb.equal(semesterUnit.get("id"), semesterUnitId));
            }

            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(root.get("status"), ResultStatus.valueOf(status)));
            }

            if (passed != null) {
                predicates.add(cb.equal(root.get("passed"), passed));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
