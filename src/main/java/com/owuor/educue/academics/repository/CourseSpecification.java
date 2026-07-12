package com.owuor.educue.academics.repository;

import com.owuor.educue.academics.entity.Course;
import org.springframework.data.jpa.domain.Specification;

public class CourseSpecification {

    public static Specification<Course> search(String search) {
        return (root, query, cb) -> {
            if (search == null || search.isBlank()) return null;

            String like = "%" + search.toLowerCase() + "%";

            return cb.or(
                    cb.like(cb.lower(root.get("name")), like),
                    cb.like(cb.lower(root.get("code")), like)
            );
        };
    }

    public static Specification<Course> status(String status) {
        return (root, query, cb) -> {
            if (status == null) return null;

            if (status.equals("active")) return cb.equal(root.get("active"), true);
            if (status.equals("inactive")) return cb.equal(root.get("active"), false);

            return null;
        };
    }

    public static Specification<Course> department(Long deptId) {
        return (root, query, cb) -> {
            if (deptId == null) return null;
            return cb.equal(root.get("department").get("id"), deptId);
        };
    }
}
