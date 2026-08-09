package com.owuor.educue.clearance.repository;
import com.owuor.educue.clearance.entity.ClearanceCheck; import com.owuor.educue.clearance.enums.*; import org.springframework.data.jpa.repository.*; import org.springframework.data.jpa.repository.JpaSpecificationExecutor; import org.springframework.data.jpa.domain.Specification; import org.springframework.data.domain.*; import java.util.List;
public interface ClearanceCheckRepository extends JpaRepository<ClearanceCheck,Long>,JpaSpecificationExecutor<ClearanceCheck>{
 @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
 @Query("select c from ClearanceCheck c where c.id=:id")
 java.util.Optional<ClearanceCheck> findByIdForUpdate(Long id);
 boolean existsByApplicationIdAndStageAndMandatoryTrueAndStatusNot(Long applicationId, ClearanceDepartmentStage stage, ClearanceCheckStatus status);
 boolean existsByApplicationIdAndStage(Long applicationId, ClearanceDepartmentStage stage);
 @EntityGraph(attributePaths={"application","application.enrollment","application.enrollment.student","application.enrollment.course","application.academicYear","department"})
 List<ClearanceCheck> findByDepartmentIdOrderByApplicationAppliedAtAsc(Long departmentId);
 @EntityGraph(attributePaths={"application","application.enrollment","application.enrollment.student","application.enrollment.course","application.academicYear","department"})
 List<ClearanceCheck> findAllByOrderByApplicationAppliedAtAsc();
 @Override @EntityGraph(attributePaths={"application","application.enrollment","application.enrollment.student","application.enrollment.course","application.academicYear","department"})
 Page<ClearanceCheck> findAll(Specification<ClearanceCheck> spec,Pageable pageable);
}
