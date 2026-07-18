package com.owuor.educue.students.repository;

import com.owuor.educue.students.entity.ExamCard;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ExamCardRepository extends JpaRepository<ExamCard, Long> {
    Optional<ExamCard> findByStudentIdAndCourseAcademicPeriodId(Long studentId, Long courseAcademicPeriodId);

    @EntityGraph(attributePaths = {"student", "courseAcademicPeriod.course", "courseAcademicPeriod.academicPeriod"})
    Optional<ExamCard> findByVerificationCode(UUID verificationCode);
}
