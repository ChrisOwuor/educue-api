package com.owuor.educue.academics.repository;

import com.owuor.educue.academics.entity.CourseAcademicPeriod;
import com.owuor.educue.institution.entity.AcademicYear;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CourseAcademicPeriodRepository extends JpaRepository<CourseAcademicPeriod, Long> {

    @EntityGraph(attributePaths = {"course", "academicPeriod"})
    Optional<CourseAcademicPeriod> findByUuid(UUID uuid);


    @EntityGraph(attributePaths = {"academicPeriod", "nextPeriod"})
    List<CourseAcademicPeriod> findByCourseIdOrderByPosition(Long courseId);

    @EntityGraph(attributePaths = {"course", "academicPeriod"})
    Optional<CourseAcademicPeriod> findFirstByCourseIdOrderByPositionAsc(Long courseId);

    @EntityGraph(attributePaths = {
            "course",
            "academicPeriod"
    })
    @Query("""
            select cap
            from CourseAcademicPeriod cap
            where cap.uuid = :uuid
            """)
    Optional<CourseAcademicPeriod> findDetailedByUuid(
            @Param("uuid") UUID uuid
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {
            "course",
            "academicPeriod"
    })
    @Query("""
            select cap
            from CourseAcademicPeriod cap
            where cap.uuid = :uuid
            """)
    Optional<CourseAcademicPeriod> findDetailedByUuidForUpdate(
            @Param("uuid") UUID uuid
    );






}
