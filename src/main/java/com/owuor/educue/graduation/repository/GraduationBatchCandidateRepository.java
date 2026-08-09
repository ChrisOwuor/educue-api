package com.owuor.educue.graduation.repository;
import com.owuor.educue.graduation.entity.GraduationBatchCandidate; import com.owuor.educue.graduation.enums.GraduationBatchCandidateStatus; import org.springframework.data.jpa.repository.*; import org.springframework.data.domain.*; import java.util.*;
public interface GraduationBatchCandidateRepository extends JpaRepository<GraduationBatchCandidate,Long>,JpaSpecificationExecutor<GraduationBatchCandidate>{
 @EntityGraph(attributePaths={"application","application.enrollment","application.enrollment.student","application.enrollment.course","application.enrollment.course.department"})
 Page<GraduationBatchCandidate> findByBatchId(Long batchId,Pageable pageable);
 @Override @EntityGraph(attributePaths={"application","application.enrollment","application.enrollment.student","application.enrollment.course","application.enrollment.course.department"})
 Page<GraduationBatchCandidate> findAll(org.springframework.data.jpa.domain.Specification<GraduationBatchCandidate> spec,Pageable pageable);
 @EntityGraph(attributePaths={"application","application.enrollment","application.enrollment.student","application.enrollment.course","application.enrollment.course.department"})
 List<GraduationBatchCandidate> findByBatchIdOrderByApplicationEnrollmentCourseDepartmentNameAscApplicationEnrollmentCourseNameAscApplicationEnrollmentStudentFullNameAsc(Long batchId);
 List<GraduationBatchCandidate> findByBatchIdAndStatusOrderByIdAsc(Long batchId,GraduationBatchCandidateStatus status);
 long countByBatchIdAndStatus(Long batchId,GraduationBatchCandidateStatus status);
 boolean existsByBatchIdAndApplicationId(Long batchId,Long applicationId);
 Optional<GraduationBatchCandidate> findByBatchIdAndApplicationId(Long batchId,Long applicationId);
}
