package com.owuor.educue.academics.service;

import com.owuor.educue.academics.dto.CourseUnitAllocationResponse;
import com.owuor.educue.academics.dto.CreateTrainerAssignmentRequest;
import com.owuor.educue.academics.dto.TrainerAssignmentResponse;
import com.owuor.educue.academics.entity.CourseUnitPlacement;
import com.owuor.educue.academics.entity.LecturerUnitAssignment;
import com.owuor.educue.academics.repository.CourseUnitPlacementRepository;
import com.owuor.educue.academics.repository.LecturerUnitAssignmentRepository;
import com.owuor.educue.institution.entity.AcademicYear;
import com.owuor.educue.institution.repository.AcademicYearRepository;
import com.owuor.educue.users.entity.User;
import com.owuor.educue.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class TrainerAssignmentService {

    private final LecturerUnitAssignmentRepository assignmentRepository;
    private final CourseUnitPlacementRepository placementRepository;
    private final AcademicYearRepository academicYearRepository;
    private final UserRepository userRepository;

    public TrainerAssignmentResponse create(CreateTrainerAssignmentRequest request, User assignedBy) {
        User lecturer = userRepository.findById(request.getLecturerId())
                .orElseThrow(() -> new IllegalArgumentException("Lecturer not found."));
        if (lecturer.getRole() == null || !"TRAINER".equals(lecturer.getRole().getName())) {
            throw new IllegalArgumentException("The selected user does not have the TRAINER role.");
        }
        if (!lecturer.isActive()) {
            throw new IllegalArgumentException("The selected lecturer account is inactive.");
        }

        CourseUnitPlacement placement = placementRepository.findByUuid(request.getCourseUnitPlacementUuid())
                .orElseThrow(() -> new IllegalArgumentException("Course unit placement not found."));
        if (!placement.isActive()) {
            throw new IllegalArgumentException("The selected course unit placement is inactive.");
        }

        AcademicYear fromYear = findYear(request.getEffectiveFromAcademicYearUuid());
        AcademicYear toYear = request.getEffectiveToAcademicYearUuid() == null
                ? null : findYear(request.getEffectiveToAcademicYearUuid());
        if (toYear != null && toYear.getStartYear() < fromYear.getStartYear()) {
            throw new IllegalArgumentException("Effective-to academic year cannot precede effective-from academic year.");
        }

        List<LecturerUnitAssignment> overlapping = placement.getLecturerAssignments().stream()
                .filter(LecturerUnitAssignment::isEnabled)
                .filter(item -> rangesOverlap(item, fromYear, toYear))
                .toList();
        if (overlapping.stream().anyMatch(item -> item.getLecturer().getId().equals(lecturer.getId()))) {
            throw new IllegalArgumentException("This lecturer is already allocated to this unit for that academic-year range.");
        }

        // A placed unit has one responsible lecturer in an academic year.
        // Replacing a lecturer disables the overlapping allocation but keeps
        // it as immutable allocation history.
        overlapping.forEach(item -> {
            item.setEnabled(false);
            assignmentRepository.save(item);
        });

        LecturerUnitAssignment assignment = new LecturerUnitAssignment();
        assignment.setCourseUnitPlacement(placement);
        assignment.setLecturer(lecturer);
        assignment.setEffectiveFromAcademicYear(fromYear);
        assignment.setEffectiveToAcademicYear(toYear);
        assignment.setStartYear(fromYear.getStartYear());
        assignment.setAssignedBy(assignedBy);
        assignment.setEnabled(true);
        return toResponse(assignmentRepository.save(assignment), currentYearOr(fromYear));
    }

    @Transactional(readOnly = true)
    public List<CourseUnitAllocationResponse> getAllocationView(String search) {
        AcademicYear currentYear = academicYearRepository.findByCurrentTrue()
                .orElseThrow(() -> new IllegalStateException("No current academic year is configured."));
        String term = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
        return placementRepository.findAllocationView().stream()
                .filter(placement -> term.isEmpty() || allocationMatches(placement, term, currentYear))
                .map(placement -> toAllocationResponse(placement, currentYear))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TrainerAssignmentResponse> getMine(Long lecturerId) {
        AcademicYear currentYear = academicYearRepository.findByCurrentTrue()
                .orElseThrow(() -> new IllegalStateException("No current academic year is configured."));
        return assignmentRepository.findByLecturerIdOrderByAssignedAtDesc(lecturerId).stream()
                .filter(item -> item.isActiveFor(currentYear))
                .map(item -> toResponse(item, currentYear))
                .toList();
    }

    private Optional<LecturerUnitAssignment> activeAssignment(CourseUnitPlacement placement, AcademicYear year) {
        return placement.getLecturerAssignments().stream()
                .filter(item -> item.isActiveFor(year))
                .max(Comparator.comparing(LecturerUnitAssignment::getAssignedAt));
    }

    private CourseUnitAllocationResponse toAllocationResponse(CourseUnitPlacement placement, AcademicYear year) {
        var coursePeriod = placement.getCourseAcademicPeriod();
        var course = coursePeriod.getCourse();
        var period = coursePeriod.getAcademicPeriod();
        var assignment = activeAssignment(placement, year).orElse(null);
        var lecturer = assignment == null ? null : assignment.getLecturer();
        var assignedBy = assignment == null ? null : assignment.getAssignedBy();
        var from = assignment == null ? null : assignment.getEffectiveFromAcademicYear();
        var to = assignment == null ? null : assignment.getEffectiveToAcademicYear();
        return CourseUnitAllocationResponse.builder()
                .courseUnitPlacementUuid(placement.getUuid())
                .assignmentId(assignment == null ? null : assignment.getId())
                .lecturerId(lecturer == null ? null : lecturer.getId())
                .lecturerName(lecturer == null ? null : lecturer.getFullName())
                .unitId(placement.getUnit().getId()).unitCode(placement.getUnit().getCode())
                .unitName(placement.getUnit().getName())
                .courseAcademicPeriodId(coursePeriod.getId()).courseAcademicPeriodUuid(coursePeriod.getUuid())
                .academicPeriodName(period.getName())
                .courseId(course.getId()).courseUuid(course.getUuid()).courseName(course.getName())
                .effectiveFromAcademicYearUuid(from == null ? null : from.getUuid())
                .effectiveFromAcademicYearCode(from == null ? null : from.getCode())
                .effectiveToAcademicYearUuid(to == null ? null : to.getUuid())
                .effectiveToAcademicYearCode(to == null ? null : to.getCode())
                .assignedAt(assignment == null ? null : assignment.getAssignedAt())
                .assignedById(assignedBy == null ? null : assignedBy.getId())
                .assignedByName(assignedBy == null ? null : assignedBy.getFullName())
                .build();
    }

    private TrainerAssignmentResponse toResponse(LecturerUnitAssignment assignment, AcademicYear currentYear) {
        var placement = assignment.getCourseUnitPlacement();
        var coursePeriod = placement.getCourseAcademicPeriod();
        var course = coursePeriod.getCourse();
        var period = coursePeriod.getAcademicPeriod();
        var assignedBy = assignment.getAssignedBy();
        return TrainerAssignmentResponse.builder()
                .id(assignment.getId()).trainerId(assignment.getLecturer().getId())
                .trainerName(assignment.getLecturer().getFullName())
                .courseUnitPlacementId(placement.getId()).courseUnitPlacementUuid(placement.getUuid())
                .unitId(placement.getUnit().getId()).unitCode(placement.getUnit().getCode())
                .unitName(placement.getUnit().getName())
                .courseAcademicPeriodId(coursePeriod.getId()).academicPeriodName(period.getName())
                .courseId(course.getId()).courseName(course.getName())
                .effectiveFromAcademicYearUuid(assignment.getEffectiveFromAcademicYear().getUuid())
                .effectiveFrom(assignment.getEffectiveFromAcademicYear().getCode())
                .effectiveToAcademicYearUuid(assignment.getEffectiveToAcademicYear() == null ? null : assignment.getEffectiveToAcademicYear().getUuid())
                .effectiveTo(assignment.getEffectiveToAcademicYear() == null ? null : assignment.getEffectiveToAcademicYear().getCode())
                .active(assignment.isActiveFor(currentYear)).assignedAt(assignment.getAssignedAt())
                .assignedById(assignedBy == null ? null : assignedBy.getId())
                .assignedByName(assignedBy == null ? null : assignedBy.getFullName()).build();
    }

    private boolean allocationMatches(CourseUnitPlacement placement, String term, AcademicYear year) {
        var course = placement.getCourseAcademicPeriod().getCourse();
        var period = placement.getCourseAcademicPeriod().getAcademicPeriod();
        return course.getName().toLowerCase(Locale.ROOT).contains(term)
                || course.getCode().toLowerCase(Locale.ROOT).contains(term)
                || period.getName().toLowerCase(Locale.ROOT).contains(term)
                || placement.getUnit().getName().toLowerCase(Locale.ROOT).contains(term)
                || placement.getUnit().getCode().toLowerCase(Locale.ROOT).contains(term)
                || activeAssignment(placement, year)
                    .map(item -> item.getLecturer().getFullName().toLowerCase(Locale.ROOT).contains(term))
                    .orElse(false);
    }

    private boolean rangesOverlap(LecturerUnitAssignment existing, AcademicYear from, AcademicYear to) {
        int newEnd = to == null ? Integer.MAX_VALUE : to.getStartYear();
        int oldEnd = existing.getEffectiveToAcademicYear() == null
                ? Integer.MAX_VALUE : existing.getEffectiveToAcademicYear().getStartYear();
        return existing.getEffectiveFromAcademicYear().getStartYear() <= newEnd
                && from.getStartYear() <= oldEnd;
    }

    private AcademicYear findYear(java.util.UUID uuid) {
        return academicYearRepository.findByUuid(uuid)
                .orElseThrow(() -> new IllegalArgumentException("Academic year not found."));
    }

    private AcademicYear currentYearOr(AcademicYear fallback) {
        return academicYearRepository.findByCurrentTrue().orElse(fallback);
    }
}
