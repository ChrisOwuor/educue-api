package com.owuor.educue.academics.repository;

import com.owuor.educue.academics.entity.TrainerAssignment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TrainerAssignmentRepository extends
        JpaRepository<TrainerAssignment, Long>,
        JpaSpecificationExecutor<TrainerAssignment> {

    Optional<TrainerAssignment> findById(Long id);

    Optional<TrainerAssignment> findBySemesterUnitIdAndEffectiveToIsNull(Long semesterUnitId);

    boolean existsByTrainerIdAndSemesterUnitIdAndEffectiveToIsNull(
            Long trainerId,
            Long semesterUnitId
    );

    @Override
    @EntityGraph(attributePaths = {
            "trainer",
            "assignedBy",
            "semesterUnit",
            "semesterUnit.unit",
            "semesterUnit.semester",
            "semesterUnit.semester.courseCurriculum",
            "semesterUnit.semester.courseCurriculum.course"
    })
    Page<TrainerAssignment> findAll(
            Specification<TrainerAssignment> specification,
            Pageable pageable
    );



    @EntityGraph(attributePaths = {
            "trainer",
            "assignedBy",
            "semesterUnit",
            "semesterUnit.unit",
            "semesterUnit.semester",
            "semesterUnit.semester.courseCurriculum",
            "semesterUnit.semester.courseCurriculum.course"
    })
    List<TrainerAssignment> findByTrainerId(Long trainerId);


    @Query("""
            select ta
            from TrainerAssignment ta
            join fetch ta.trainer
            join fetch ta.semesterUnit su
            where su.semester.id = :semesterId
            and ta.effectiveTo is null
            """)
    List<TrainerAssignment> findActiveAssignmentsBySemester(
            @Param("semesterId") Long semesterId
    );

    Optional<TrainerAssignment> findFirstBySemesterUnitIdAndEffectiveToIsNull(
            Long semesterUnitId
    );

    List<TrainerAssignment> findByTrainerIdAndEffectiveToIsNullOrderByAssignedAtDesc(
            Long trainerId
    );

    boolean existsBySemesterUnitIdAndEffectiveToIsNull(
            Long semesterUnitId
    );

}
