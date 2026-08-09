package com.owuor.educue.certificate.repository;
import com.owuor.educue.certificate.entity.GraduationCertificate;import org.springframework.data.jpa.repository.*;import org.springframework.data.jpa.repository.JpaSpecificationExecutor;import org.springframework.data.jpa.domain.Specification;import org.springframework.data.domain.*;import java.util.*;
public interface GraduationCertificateRepository extends JpaRepository<GraduationCertificate,Long>,JpaSpecificationExecutor<GraduationCertificate>{
 @EntityGraph(attributePaths={"job","application","application.enrollment","application.enrollment.student","application.enrollment.course"})Page<GraduationCertificate> findByJobId(Long jobId,Pageable pageable);
 @Override @EntityGraph(attributePaths={"job","application","application.enrollment","application.enrollment.student","application.enrollment.course"})Page<GraduationCertificate> findAll(Specification<GraduationCertificate> spec,Pageable pageable);
 Optional<GraduationCertificate> findByApplicationId(Long applicationId);
 @EntityGraph(attributePaths={"job","application"})Optional<GraduationCertificate> findByUuid(UUID uuid);
 @EntityGraph(attributePaths={"job","application","application.enrollment","application.enrollment.student","application.enrollment.course"})Optional<GraduationCertificate> findByApplicationEnrollmentStudentUserId(Long userId);
}
