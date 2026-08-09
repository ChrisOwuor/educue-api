package com.owuor.educue.clearance.repository;
import com.owuor.educue.clearance.entity.ClearanceApplication; import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface ClearanceApplicationRepository extends JpaRepository<ClearanceApplication,Long>{
 Optional<ClearanceApplication> findFirstByEnrollmentIdOrderByAppliedAtDesc(Long enrollmentId);
 Optional<ClearanceApplication> findByEnrollmentIdAndAcademicYearId(Long enrollmentId,Long academicYearId);
}
