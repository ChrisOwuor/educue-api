package com.owuor.educue.academics.service;

import com.owuor.educue.academics.dto.CreateCourseCurriculumRequest;
import com.owuor.educue.academics.dto.CourseCurriculumResponse;
import com.owuor.educue.academics.dto.CreateCurriculumWithStructureRequest;
import com.owuor.educue.academics.entity.Course;
import com.owuor.educue.academics.entity.CourseCurriculum;
import com.owuor.educue.academics.entity.Semester;
import com.owuor.educue.academics.entity.SemesterUnit;
import com.owuor.educue.academics.repository.CourseCurriculumRepository;
import com.owuor.educue.academics.repository.CourseRepository;
import com.owuor.educue.academics.repository.SemesterRepository;
import com.owuor.educue.academics.repository.SemesterUnitRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.apache.tomcat.util.http.fileupload.ByteArrayOutputStream;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;



@Service
@RequiredArgsConstructor
public class  CourseCurriculumService {

    private final CourseRepository courseRepository;
    private final CourseCurriculumRepository curriculumRepository;
    private final SemesterRepository semesterRepository;
    private final SemesterUnitRepository semesterUnitRepository;


    public CourseCurriculumResponse create(
            CreateCourseCurriculumRequest request
    ) {

        Course course = courseRepository.findById(
                request.getCourseId()
        ).orElseThrow(() ->
                new IllegalArgumentException("Course not found")
        );

        CourseCurriculum curriculum = new CourseCurriculum();

        curriculum.setCourse(course);
        curriculum.setName(request.getName());

        curriculum.setActive(
                request.getActive() != null
                        ? request.getActive()
                        : true
        );

        curriculum.setDefaultForAdmission(
                request.getDefaultForAdmission() != null
                        ? request.getDefaultForAdmission()
                        : false
        );

        curriculum.setEffectiveFrom(
                request.getEffectiveFrom()
        );

        curriculum.setEffectiveTo(
                request.getEffectiveTo()
        );

        curriculum = curriculumRepository.save(curriculum);

        return map(curriculum);
    }


    public List<CourseCurriculumResponse> getByCourse(
            Long courseId
    ) {

        return curriculumRepository
                .findByCourseIdOrderByCreatedAtDesc(courseId)
                .stream()
                .map(this::map)
                .toList();
    }


    public CourseCurriculumResponse getById(
            Long curriculumId
    ) {

        CourseCurriculum curriculum =
                curriculumRepository.findById(curriculumId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Curriculum not found"
                                )
                        );

        return map(curriculum);
    }

    private CourseCurriculumResponse map(
            CourseCurriculum curriculum
    ) {

        return CourseCurriculumResponse.builder()
                .id(curriculum.getId())
                .courseId(curriculum.getCourse().getId())
                .courseName(curriculum.getCourse().getName())
                .name(curriculum.getName())
                .active(curriculum.isActive())
                .defaultForAdmission(
                        curriculum.isDefaultForAdmission()
                )
                .effectiveFrom(curriculum.getEffectiveFrom())
                .effectiveTo(curriculum.getEffectiveTo())
                .build();
    }

    //Stale version
    @Transactional
    public CourseCurriculum createWithSemestersStale(CreateCurriculumWithStructureRequest request) {

        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new RuntimeException("Course not found"));

        CourseCurriculum curriculum = new CourseCurriculum();
        curriculum.setCourse(course);
        curriculum.setName(request.getName());
        curriculum.setDefaultForAdmission(Boolean.TRUE.equals(request.getDefaultForAdmission()));
        curriculum.setActive(true);
        curriculum.setEffectiveFrom(request.getEffectiveFrom());
        curriculum.setEffectiveTo(request.getEffectiveTo());

        curriculum = curriculumRepository.save(curriculum);

        List<Semester> semesters = new ArrayList<>();

        for (int y = 1; y <= request.getTotalYears(); y++) {
            for (int s = 1; s <= request.getSemestersPerYear(); s++) {

                Semester semester = new Semester();
                semester.setCourseCurriculum(curriculum);
                semester.setYearNumber(y);
                semester.setSemesterNumber(s);
                semester.setName("Year " + y + " Semester " + s);

                semesters.add(semester);
            }
        }

        semesterRepository.saveAll(semesters);

        return curriculum;
    }


    @Transactional
    public CourseCurriculum createWithSemesters(
            CreateCurriculumWithStructureRequest request
    ) {

        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() ->
                        new RuntimeException("Course not found"));

        CourseCurriculum curriculum = new CourseCurriculum();
        curriculum.setCourse(course);
        curriculum.setName(request.getName());
        curriculum.setDefaultForAdmission(
                Boolean.TRUE.equals(request.getDefaultForAdmission()));
        curriculum.setActive(true);
        curriculum.setEffectiveFrom(request.getEffectiveFrom());
        curriculum.setEffectiveTo(request.getEffectiveTo());

        curriculum = curriculumRepository.save(curriculum);

        List<Semester> semesters = new ArrayList<>();

        // ----------------------------------------
        // Create all semesters
        // ----------------------------------------
        for (int year = 1; year <= request.getTotalYears(); year++) {

            for (int sem = 1; sem <= request.getSemestersPerYear(); sem++) {

                Semester semester = new Semester();

                semester.setCourseCurriculum(curriculum);
                semester.setYearNumber(year);
                semester.setSemesterNumber(sem);
                semester.setName(
                        "Year " + year + " Semester " + sem
                );

                semesters.add(semester);
            }
        }

        // ----------------------------------------
        // Link next semesters
        // ----------------------------------------
        for (int i = 0; i < semesters.size() - 1; i++) {

            semesters.get(i)
                    .setNextSemester(semesters.get(i + 1));
        }

        semesterRepository.saveAll(semesters);

        // ----------------------------------------
        // Save first semester
        // ----------------------------------------
        curriculum.setFirstSemester(
                semesters.getFirst()
        );

        curriculumRepository.save(curriculum);

        return curriculum;
    }


    public byte[] generatePdf(Long curriculumId) {

        CourseCurriculum curriculum = curriculumRepository.findById(curriculumId)
                .orElseThrow();

        List<Semester> semesters = semesterRepository.findByCourseCurriculumId(curriculumId);

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        Document document = new Document();
        PdfWriter.getInstance(document, out);

        document.open();

        // Title
        document.add(new Paragraph("Curriculum: " + curriculum.getName()));
        document.add(new Paragraph(" "));

        for (Semester semester : semesters) {

            document.add(new Paragraph(
                    "Year " + semester.getYearNumber() +
                            " Semester " + semester.getSemesterNumber()
            ));

            List<SemesterUnit> units =
                    semesterUnitRepository.findBySemesterId(semester.getId());

            for (SemesterUnit su : units) {
                document.add(new Paragraph(
                        "- " + su.getUnit().getCode()
                                + " | " + su.getUnit().getName()
                                + " | " + su.getCategory()
                                + " | " + (su.isMandatory() ? "Mandatory" : "Optional")
                ));
            }

            document.add(new Paragraph(" "));
        }

        document.close();

        return out.toByteArray();
    }
}
