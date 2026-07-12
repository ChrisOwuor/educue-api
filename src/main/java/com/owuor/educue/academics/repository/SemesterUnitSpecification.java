package com.owuor.educue.academics.repository;

import com.owuor.educue.academics.entity.CourseCurriculum;
import com.owuor.educue.academics.entity.Semester;
import com.owuor.educue.academics.entity.SemesterUnit;
import com.owuor.educue.academics.entity.Unit;
import jakarta.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class SemesterUnitSpecification {

    private SemesterUnitSpecification() {
    }

    public static Specification<SemesterUnit> withFilters(

            String search,
            Long courseId,
            Long curriculumId,
            Long semesterId

    ) {

        return (root, query, cb) -> {

            List<jakarta.persistence.criteria.Predicate> predicates =
                    new ArrayList<>();

            Join<SemesterUnit, Unit> unit =
                    root.join("unit");

            Join<SemesterUnit, Semester> semester =
                    root.join("semester");

            Join<Semester, CourseCurriculum> curriculum =
                    semester.join("courseCurriculum");

            //---------------------------------------------------------
            // SEARCH
            //---------------------------------------------------------

            if (search != null && !search.isBlank()) {

                String pattern = "%" + search.toLowerCase() + "%";

                predicates.add(

                        cb.or(

                                cb.like(
                                        cb.lower(unit.get("name")),
                                        pattern
                                ),

                                cb.like(
                                        cb.lower(unit.get("code")),
                                        pattern
                                )

                        )
                );
            }

            //---------------------------------------------------------
            // COURSE
            //---------------------------------------------------------

            if (courseId != null) {

                predicates.add(

                        cb.equal(

                                curriculum
                                        .join("course")
                                        .get("id"),

                                courseId
                        )
                );
            }

            //---------------------------------------------------------
            // CURRICULUM
            //---------------------------------------------------------

            if (curriculumId != null) {

                predicates.add(

                        cb.equal(
                                curriculum.get("id"),
                                curriculumId
                        )
                );
            }

            //---------------------------------------------------------
            // SEMESTER
            //---------------------------------------------------------

            if (semesterId != null) {

                predicates.add(

                        cb.equal(
                                semester.get("id"),
                                semesterId
                        )
                );
            }

            query.distinct(true);

            return cb.and(
                    predicates.toArray(
                            new jakarta.persistence.criteria.Predicate[0]
                    )
            );
        };
    }
}
