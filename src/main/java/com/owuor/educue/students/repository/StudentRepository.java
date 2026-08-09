package com.owuor.educue.students.repository;

import com.owuor.educue.students.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.Query;

public interface StudentRepository extends JpaRepository<Student,Long> {

    Optional<Student> findByAdmissionNumberIgnoreCase(String admissionNumber);
    @Query("select distinct e.student from Enrollment e where e.status = com.owuor.educue.students.enums.EnrollmentStatus.ACTIVE")
    List<Student> findAllWithActiveEnrollment();

}
