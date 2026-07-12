package com.owuor.educue.finance.service;

import com.owuor.educue.academics.entity.Course;
import com.owuor.educue.academics.entity.Semester;
import com.owuor.educue.academics.repository.CourseRepository;
import com.owuor.educue.academics.repository.SemesterRepository;
import com.owuor.educue.admissions.entity.Intake;
import com.owuor.educue.admissions.repository.IntakeRepository;
import com.owuor.educue.finance.dto.CreateFeeStructureItemRequest;
import com.owuor.educue.finance.dto.CreateFeeStructureRequest;
import com.owuor.educue.finance.dto.FeeStructureItemResponse;
import com.owuor.educue.finance.dto.FeeStructureResponse;
import com.owuor.educue.finance.entity.FeeStructure;
import com.owuor.educue.finance.entity.FeeStructureItem;
import com.owuor.educue.finance.repository.FeeStructureRepository;
import com.owuor.educue.students.entity.Enrollment;
import com.owuor.educue.students.repository.EnrollmentRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class FeeStructureService {

    private final FeeStructureRepository feeStructureRepository;
    private final IntakeRepository intakeRepository;
    private final CourseRepository courseRepository;
    private final SemesterRepository semesterRepository;
    private final EnrollmentRepository enrollmentRepository;


    public FeeStructureResponse create(CreateFeeStructureRequest request) {

        if (feeStructureRepository.existsByIntakeIdAndCourseIdAndSemesterId(request.getIntakeId(), request.getCourseId(), request.getSemesterId())) {

            throw new IllegalArgumentException("Fee structure already exists.");
        }

        Intake intake = intakeRepository.findById(request.getIntakeId()).orElseThrow(() -> new EntityNotFoundException("Intake not found"));


        Course course = courseRepository.findById(request.getCourseId()).orElseThrow(() -> new EntityNotFoundException("Course not found"));

        Semester semester = semesterRepository.findById(request.getSemesterId()).orElseThrow(() -> new EntityNotFoundException("Semester not found"));

        FeeStructure structure = new FeeStructure();

        structure.setIntake(intake);
        structure.setCourse(course);
        structure.setSemester(semester);
        BigDecimal total = BigDecimal.ZERO;

        for (CreateFeeStructureItemRequest itemRequest : request.getItems()) {

            FeeStructureItem item = new FeeStructureItem();
            item.setFeeStructure(structure);
            item.setName(itemRequest.getName());
            item.setAmount(itemRequest.getAmount());

            total = total.add(itemRequest.getAmount());

            structure.getItems().add(item);
        }
        structure.setTotalAmount(total);


        structure = feeStructureRepository.save(structure);

        return toResponse(structure);
    }

    @Transactional(readOnly = true)
    public FeeStructureResponse get(Long id) {

        FeeStructure structure = feeStructureRepository.findWithItemsById(id).orElseThrow(() -> new EntityNotFoundException("Fee structure not found"));

        return toResponse(structure);
    }

    @Transactional(readOnly = true)
    public List<FeeStructureResponse> getAll() {

        return feeStructureRepository.findAll().stream().map(this::toResponse).toList();
    }


    public void delete(Long id) {

        if (!feeStructureRepository.existsById(id)) {
            throw new EntityNotFoundException("Fee structure not found");
        }

        feeStructureRepository.deleteById(id);
    }

    public FeeStructureResponse getStudentFeeStructure(Long studentUserId) {
        Enrollment enrollment = enrollmentRepository.findByStudentUserId(studentUserId)
                .orElseThrow(() -> new EntityNotFoundException("Student profile not found"));

        Long intakeId = enrollment.getStudent().getApplication().getIntake().getId();
        Long courseId = enrollment.getCourse().getId();
        Long semesterId = enrollment.getCurrentSemester().getId();

        FeeStructure structure = feeStructureRepository.findByIntakeIdAndCourseIdAndSemesterId(intakeId, courseId, semesterId)
                .orElseThrow(() -> new EntityNotFoundException("Fee structure not found for this student's intake, course, and semester."));

        return toResponse(structure);
    }

    public FeeStructureResponse toResponse(FeeStructure structure) {

        BigDecimal total = structure.getItems().stream().map(FeeStructureItem::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        return FeeStructureResponse.builder().id(structure.getId()).intakeId(structure.getIntake().getId()).intakeName(structure.getIntake().getName()).courseId(structure.getCourse().getId()).courseName(structure.getCourse().getName()).semesterId(structure.getSemester().getId()).semesterName(structure.getSemester().getName()).total(total).items(structure.getItems().stream().map(item -> FeeStructureItemResponse.builder().id(item.getId()).name(item.getName()).amount(item.getAmount()).build()).toList()).build();
    }

}
