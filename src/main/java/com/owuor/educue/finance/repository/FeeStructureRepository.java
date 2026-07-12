package com.owuor.educue.finance.repository;

import com.owuor.educue.finance.entity.FeeStructure;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FeeStructureRepository extends JpaRepository<FeeStructure, Long> {

    boolean existsByIntakeIdAndCourseIdAndSemesterId(
            Long intakeId,
            Long courseId,
            Long semesterId
    );

    @EntityGraph(attributePaths = {
            "intake",
            "course",
            "semester",
            "items"
    })
    Optional<FeeStructure> findWithItemsById(Long id);

    @EntityGraph(attributePaths = {
            "intake",
            "course",
            "semester",
            "items"
    })
    Optional<FeeStructure> findByIntakeIdAndCourseIdAndSemesterId(Long intakeId, Long courseId, Long semesterId);
}
