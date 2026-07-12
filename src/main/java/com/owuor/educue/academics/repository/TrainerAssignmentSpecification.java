package com.owuor.educue.academics.repository;

import com.owuor.educue.academics.entity.CourseCurriculum;
import com.owuor.educue.academics.entity.Semester;
import com.owuor.educue.academics.entity.SemesterUnit;
import com.owuor.educue.academics.entity.TrainerAssignment;
import com.owuor.educue.users.entity.User;
import jakarta.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class TrainerAssignmentSpecification {

    private TrainerAssignmentSpecification() {
    }

    public static Specification<TrainerAssignment> withFilters(

            String search,
            Long trainerId,
            Long courseId,
            Long curriculumId,
            Long semesterId,
            Boolean activeOnly

    ) {

        return (root, query, cb) -> {

            List<jakarta.persistence.criteria.Predicate> predicates =
                    new ArrayList<>();

            Join<TrainerAssignment, User> trainer =
                    root.join("trainer");

            Join<TrainerAssignment, SemesterUnit> semesterUnit =
                    root.join("semesterUnit");

            Join<SemesterUnit, Semester> semester =
                    semesterUnit.join("semester");

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
                                        cb.lower(trainer.get("firstName")),
                                        pattern
                                ),

                                cb.like(
                                        cb.lower(trainer.get("lastName")),
                                        pattern
                                ),

                                cb.like(
                                        cb.lower(
                                                semesterUnit
                                                        .join("unit")
                                                        .get("name")
                                        ),
                                        pattern
                                ),

                                cb.like(
                                        cb.lower(
                                                semesterUnit
                                                        .join("unit")
                                                        .get("code")
                                        ),
                                        pattern
                                )
                        )
                );
            }

            //---------------------------------------------------------
            // TRAINER
            //---------------------------------------------------------

            if (trainerId != null) {

                predicates.add(
                        cb.equal(trainer.get("id"), trainerId)
                );
            }

            //---------------------------------------------------------
            // COURSE
            //---------------------------------------------------------

            if (courseId != null) {

                predicates.add(
                        cb.equal(
                                curriculum.join("course").get("id"),
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

            //---------------------------------------------------------
            // ACTIVE ONLY
            //---------------------------------------------------------

            if (Boolean.TRUE.equals(activeOnly)) {

                predicates.add(

                        cb.or(

                                cb.isNull(root.get("effectiveTo")),

                                cb.greaterThan(
                                        root.get("effectiveTo"),
                                        LocalDate.now()
                                )
                        )
                );
            }

            query.distinct(true);

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }
}
