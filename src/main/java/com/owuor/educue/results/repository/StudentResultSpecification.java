package com.owuor.educue.results.repository;

import com.owuor.educue.results.entity.StudentResult;
import com.owuor.educue.results.enums.ResultStatus;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;

public final class StudentResultSpecification {
    private StudentResultSpecification() {}

    public static Specification<StudentResult> withFilters(String search, Long courseId,
            Long courseAcademicPeriodId, Long placementId, String status, Boolean passed) {
        return (root, query, cb) -> {
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
            var registration = root.join("studentUnitRegistration");
            var enrollment = registration.join("enrollment");
            var student = enrollment.join("student");
            var placement = registration.join("courseUnitPlacement");
            var unit = placement.join("unit");
            var coursePeriod = placement.join("courseAcademicPeriod");
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.toLowerCase() + "%";
                predicates.add(cb.or(cb.like(cb.lower(student.get("fullName")), pattern),
                        cb.like(cb.lower(student.get("admissionNumber")), pattern),
                        cb.like(cb.lower(unit.get("code")), pattern), cb.like(cb.lower(unit.get("name")), pattern)));
            }
            if (courseId != null) predicates.add(cb.equal(coursePeriod.join("course").get("id"), courseId));
            if (courseAcademicPeriodId != null) predicates.add(cb.equal(coursePeriod.get("id"), courseAcademicPeriodId));
            if (placementId != null) predicates.add(cb.equal(placement.get("id"), placementId));
            if (status != null && !status.isBlank()) predicates.add(cb.equal(root.get("status"), ResultStatus.valueOf(status)));
            if (passed != null) predicates.add(cb.equal(root.get("passed"), passed));
            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }
}
