package com.owuor.educue.academics.repository;

import com.owuor.educue.academics.entity.Semester;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface SemesterRepository extends JpaRepository<Semester, Long> {

    // Get all semesters for a curriculum ordered in progression order
    List<Semester> findByCourseCurriculumIdOrderByYearNumberAscSemesterNumberAsc(Long courseCurriculumId);

    // Optional: check if a curriculum already has semesters generated
    boolean existsByCourseCurriculumId(Long courseCurriculumId);

    // Optional: delete all semesters for a curriculum (useful if regenerating)
    void deleteByCourseCurriculumId(Long courseCurriculumId);

    List<Semester> findByCourseCurriculumId(Long curriculumId);

    Optional<Semester> findFirstByCourseCurriculumIdOrderBySemesterNumberAsc(Long id);

    Optional<Semester>
    findFirstByCourseCurriculumIdOrderByYearNumberAscSemesterNumberAsc(
            Long curriculumId
    );
}
