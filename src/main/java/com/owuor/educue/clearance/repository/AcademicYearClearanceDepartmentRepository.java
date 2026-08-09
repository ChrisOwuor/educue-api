package com.owuor.educue.clearance.repository;
import com.owuor.educue.clearance.entity.AcademicYearClearanceDepartment; import org.springframework.data.jpa.repository.JpaRepository; import java.util.List;
public interface AcademicYearClearanceDepartmentRepository extends JpaRepository<AcademicYearClearanceDepartment,Long>{List<AcademicYearClearanceDepartment> findByAcademicYearIdOrderByDisplayOrderAsc(Long id);void deleteByAcademicYearId(Long id);}
