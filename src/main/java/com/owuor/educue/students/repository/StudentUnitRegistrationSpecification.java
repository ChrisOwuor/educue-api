package com.owuor.educue.students.repository;

import com.owuor.educue.academics.enums.AttemptType;
import com.owuor.educue.academics.enums.RegistrationStatus;
import com.owuor.educue.students.entity.StudentUnitRegistration;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;

public final class StudentUnitRegistrationSpecification {
    private StudentUnitRegistrationSpecification() {}

    public static Specification<StudentUnitRegistration> withFilters(String search, Long courseId,
            Long courseAcademicPeriodId, Long placementId, AttemptType attemptType, RegistrationStatus status) {
        return (root, query, cb) -> {
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
            var enrollment = root.join("enrollment");
            var student = enrollment.join("student");
            var placement = root.join("courseUnitPlacement");
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
            if (attemptType != null) predicates.add(cb.equal(root.get("attemptType"), attemptType));
            if (status != null) predicates.add(cb.equal(root.get("status"), status));
            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }
}
