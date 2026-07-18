package com.owuor.educue.students.repository;

import com.owuor.educue.students.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student,Long> {

    boolean existsByApplicationId(Long applicationId);

    Optional<Student> findByAdmissionNumberIgnoreCase(String admissionNumber);

}
