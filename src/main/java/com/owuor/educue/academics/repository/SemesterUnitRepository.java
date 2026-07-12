package com.owuor.educue.academics.repository;

import com.owuor.educue.academics.entity.SemesterUnit;
import com.owuor.educue.academics.enums.RegistrationStatus;
import com.owuor.educue.students.entity.StudentUnitRegistration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface SemesterUnitRepository
        extends JpaRepository<SemesterUnit, Long> , JpaSpecificationExecutor<SemesterUnit> {

    List<SemesterUnit> findBySemesterIdOrderByIdAsc(Long semesterId);

    boolean existsBySemesterIdAndUnitId(
            Long semesterId,
            Long unitId
    );

    List<SemesterUnit> findBySemesterId(Long id);

    @Query("""
            select su
            from Enrollment e
            join e.currentSemester s
            join SemesterUnit su on su.semester.id = s.id
            join fetch su.unit
            where e.student.user.id = :userId
              and e.status = com.owuor.educue.students.enums.EnrollmentStatus.ACTIVE
              and s.courseCurriculum = e.courseCurriculum
            """)
    List<SemesterUnit> findCurrentSemesterUnitsByUserId(
            @Param("userId") Long userId
    );

    @Override
    @EntityGraph(attributePaths = {
            "unit",
            "semester",
            "semester.courseCurriculum",
            "semester.courseCurriculum.course"
    })
    Page<SemesterUnit> findAll(
            Specification<SemesterUnit> specification,
            Pageable pageable
    );

    List<SemesterUnit> findBySemesterIdAndMandatoryTrue(Long semesterId);



}
