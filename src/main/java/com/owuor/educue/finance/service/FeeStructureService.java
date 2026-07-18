package com.owuor.educue.finance.service;

import com.owuor.educue.academics.entity.CourseAcademicPeriod;
import com.owuor.educue.academics.repository.CourseAcademicPeriodRepository;
import com.owuor.educue.admissions.entity.IntakeCourse;
import com.owuor.educue.admissions.repository.IntakeCourseRepository;
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
    private final IntakeCourseRepository intakeCourseRepository;
    private final CourseAcademicPeriodRepository courseAcademicPeriodRepository;
    private final EnrollmentRepository enrollmentRepository;


    public FeeStructureResponse create(CreateFeeStructureRequest request) {

        IntakeCourse intakeCourse = intakeCourseRepository.findById(request.getIntakeCourseId())
                .orElseThrow(() -> new EntityNotFoundException("Intake course not found"));
        CourseAcademicPeriod coursePeriod = courseAcademicPeriodRepository.findByUuid(request.getCourseAcademicPeriodUuid())
                .orElseThrow(() -> new EntityNotFoundException("Course academic period not found"));
        if (!coursePeriod.getCourse().getId().equals(intakeCourse.getCourse().getId())) {
            throw new IllegalArgumentException("Academic period does not belong to the intake course");
        }
        if (feeStructureRepository.existsByIntakeCourseIdAndCourseAcademicPeriodId(intakeCourse.getId(), coursePeriod.getId())) {
            throw new IllegalArgumentException("Fee structure already exists.");
        }

        FeeStructure structure = new FeeStructure();

        structure.setIntakeCourse(intakeCourse);
        structure.setCourseAcademicPeriod(coursePeriod);
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

        IntakeCourse intakeCourse = enrollment.getIntakeCourse();
        CourseAcademicPeriod coursePeriod = enrollment.getCurrentCourseAcademicPeriod();
        FeeStructure structure = feeStructureRepository
                .findByIntakeCourseIdAndCourseAcademicPeriodId(intakeCourse.getId(), coursePeriod.getId())
                .orElseThrow(() -> new EntityNotFoundException("Fee structure not found for this student's intake course and academic period."));

        return toResponse(structure);
    }

    @Transactional(readOnly = true)
    public List<FeeStructureResponse> getAllStudentFeeStructures(Long studentUserId) {
        var enrollment = enrollmentRepository.findByStudentUserId(studentUserId)
                .orElseThrow(() -> new EntityNotFoundException("Student enrollment not found"));
        return feeStructureRepository.findByIntakeCourseIdOrderByCourseAcademicPeriodPosition(
                enrollment.getIntakeCourse().getId()).stream().map(this::toResponse).toList();
    }

    public FeeStructureResponse toResponse(FeeStructure structure) {

        BigDecimal total = structure.getItems().stream().map(FeeStructureItem::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        var intakeCourse = structure.getIntakeCourse();
        var coursePeriod = structure.getCourseAcademicPeriod();
        var period = coursePeriod.getAcademicPeriod();
        return FeeStructureResponse.builder()
                .id(structure.getId()).intakeId(intakeCourse.getIntake().getId())
                .intakeName(intakeCourse.getIntake().getName()).intakeCourseId(intakeCourse.getId())
                .courseId(intakeCourse.getCourse().getId()).courseName(intakeCourse.getCourse().getName())
                .courseAcademicPeriodUuid(coursePeriod.getUuid()).academicPeriodCode(period.getCode())
                .academicPeriodName(period.getName()).academicPeriodPosition(coursePeriod.getPosition())
                .total(total).active(structure.isActive()).createdAt(structure.getCreatedAt())
                .items(structure.getItems().stream().map(item -> FeeStructureItemResponse.builder().id(item.getId()).name(item.getName()).amount(item.getAmount()).build()).toList()).build();
    }

}
