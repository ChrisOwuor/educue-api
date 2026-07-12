package com.owuor.educue.students.repository;

import com.owuor.educue.academics.enums.RegistrationStatus;
import com.owuor.educue.students.entity.StudentUnitRegistration;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StudentUnitRegistrationRepository
        extends JpaRepository<StudentUnitRegistration, Long>, JpaSpecificationExecutor<StudentUnitRegistration> {

    boolean existsByEnrollmentIdAndSemesterUnitIdAndStatus(
            Long enrollmentId,
            Long semesterUnitId,
            RegistrationStatus status
    );

    @Query("""
                select r
                from StudentUnitRegistration r
                    join fetch r.semesterUnit su
                    join fetch su.unit
                    join fetch su.semester
                where r.enrollment.student.user.id = :userId
                  and r.status = com.owuor.educue.academics.enums.RegistrationStatus.ACTIVE
                order by su.unit.code
            """)
    List<StudentUnitRegistration> findRegisteredUnitsByUserId(
            @Param("userId") Long userId
    );


    @Query("""
                select r
                from StudentUnitRegistration r
                    join fetch r.semesterUnit su
                    join fetch su.unit
                    join fetch su.semester
                where r.enrollment.student.user.id = :userId
                  and r.enrollment.status = com.owuor.educue.students.enums.EnrollmentStatus.ACTIVE
                  and r.status = com.owuor.educue.academics.enums.RegistrationStatus.ACTIVE
                order by su.unit.code
            """)
    List<StudentUnitRegistration> findCurrentRegisteredUnitsByUserId(
            @Param("userId") Long userId
    );

    @EntityGraph(attributePaths = {
            "enrollment",
            "enrollment.student"
    })
    List<StudentUnitRegistration>
    findBySemesterUnitIdAndStatusOrderByEnrollmentStudentAdmissionNumberAsc(
            Long semesterUnitId,
            RegistrationStatus status
    );


    @EntityGraph(attributePaths = {
            "semesterUnit",
            "semesterUnit.unit",
            "semesterUnit.semester",
            "enrollment",
            "enrollment.student"
    })
    List<StudentUnitRegistration> findBySemesterUnitIdAndStatus(
            Long semesterUnitId,
            RegistrationStatus status
    );

    @EntityGraph(attributePaths = {
            "enrollment",
            "enrollment.student",
            "enrollment.student.user",
            "enrollment.courseCurriculum",
            "enrollment.currentSemester",
            "semesterUnit",
            "semesterUnit.unit",
            "semesterUnit.semester"
    })
    List<StudentUnitRegistration> findByStatusOrderByRegisteredAtDesc(
            RegistrationStatus status
    );


    @Override
    @EntityGraph(attributePaths = {
            "semesterUnit",
            "semesterUnit.unit",
            "semesterUnit.semester",
            "semesterUnit.semester.courseCurriculum",
            "semesterUnit.semester.courseCurriculum.course",

            "enrollment",
            "enrollment.student",
            "enrollment.currentSemester",
            "enrollment.courseCurriculum",
            "enrollment.courseCurriculum.course"
    })
    org.springframework.data.domain.Page<StudentUnitRegistration> findAll(
            org.springframework.data.jpa.domain.Specification<StudentUnitRegistration> spec,
            org.springframework.data.domain.Pageable pageable
    );


    @EntityGraph(attributePaths = {
            "enrollment",
            "enrollment.student",
            "semesterUnit",
            "semesterUnit.unit",
            "semesterUnit.semester"
    })
    List<StudentUnitRegistration>
    findBySemesterUnitIdAndStatusOrderByEnrollmentStudentFullNameAsc(
            Long semesterUnitId,
            RegistrationStatus status
    );


    @EntityGraph(attributePaths = {
            "semesterUnit",
            "semesterUnit.unit"
    })
    List<StudentUnitRegistration>
    findByEnrollmentIdInAndStatus(
            List<Long> enrollmentIds,
            RegistrationStatus status
    );


    @EntityGraph(attributePaths = {
            "semesterUnit",
            "semesterUnit.unit",
            "enrollment",
            "enrollment.student"
    })
    List<StudentUnitRegistration>
    findByEnrollmentIdAndSemesterUnitSemesterIdAndStatus(
            Long enrollmentId,
            Long semesterId,
            RegistrationStatus status
    );

    List<StudentUnitRegistration>
    findByEnrollmentIdAndSemesterUnitSemesterId(
            Long enrollmentId,
            Long semesterId
    );

}
