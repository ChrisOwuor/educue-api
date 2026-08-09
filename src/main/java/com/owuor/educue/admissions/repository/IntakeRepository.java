package com.owuor.educue.admissions.repository;

import com.owuor.educue.admissions.entity.Intake;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface IntakeRepository extends JpaRepository<Intake, Long> {
    boolean existsByName(String name);
    boolean existsByNameIgnoreCase(String name);

    // "Open" here means within the application window, not just a status
    // flag - this is what the public apply page actually needs: intakes
    // whose deadline hasn't passed yet, regardless of whether someone
    // remembered to flip the status field.
    List<Intake> findByApplicationDeadlineGreaterThanEqual(LocalDate today);

    Optional<Intake> findByName(String name);

}
