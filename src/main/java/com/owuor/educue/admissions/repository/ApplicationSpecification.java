package com.owuor.educue.admissions.repository;

import com.owuor.educue.admissions.entity.Application;
import com.owuor.educue.admissions.enums.ApplicationStatus;
import org.springframework.data.jpa.domain.Specification;

public final class ApplicationSpecification {
    private ApplicationSpecification() {}

    public static Specification<Application> search(String search) {
        return (root, query, cb) -> {
            if (search == null || search.isBlank()) return null;
            String like = "%" + search.trim().toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("applicationNumber")), like),
                    cb.like(cb.lower(root.get("fullName")), like),
                    cb.like(cb.lower(root.get("email")), like),
                    cb.like(cb.lower(root.get("intakeCourse").get("course").get("name")), like),
                    cb.like(cb.lower(root.get("intakeCourse").get("course").get("code")), like),
                    cb.like(cb.lower(root.get("intakeCourse").get("intake").get("name")), like)
            );
        };
    }

    public static Specification<Application> status(ApplicationStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }
}
