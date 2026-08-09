package com.owuor.educue.graduation.service;

import com.owuor.educue.academics.entity.*;
import com.owuor.educue.academics.enums.UnitType;
import com.owuor.educue.academics.repository.CourseUnitPlacementRepository;
import com.owuor.educue.clearance.entity.ClearanceCheck;
import com.owuor.educue.clearance.enums.ClearanceCheckStatus;
import com.owuor.educue.clearance.repository.*;
import com.owuor.educue.graduation.dto.GraduationReadinessResponse;
import com.owuor.educue.graduation.dto.GraduationReadinessResponse.Blocker;
import com.owuor.educue.results.entity.StudentResult;
import com.owuor.educue.results.enums.ResultStatus;
import com.owuor.educue.results.repository.StudentResultRepository;
import com.owuor.educue.students.entity.Enrollment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class GraduationReadinessService {
    private final CourseUnitPlacementRepository placementRepository;
    private final StudentResultRepository resultRepository;
    private final ClearanceApplicationRepository clearanceRepository;
    private final AcademicYearClearanceDepartmentRepository clearanceConfigRepository;

    @Transactional(readOnly = true)
    public GraduationReadinessResponse assess(Enrollment e) {
        List<CourseUnitPlacement> placements = placementRepository.findEffectiveForCourseAndIntake(e.getCourse().getId(), e.getIntake().getSequenceNumber());
        List<StudentResult> results = resultRepository.findByStudentUnitRegistrationEnrollmentId(e.getId());
        Map<Long, List<StudentResult>> byUnit = new HashMap<>();
        for (var r : results)
            byUnit.computeIfAbsent(r.getStudentUnitRegistration().getCourseUnitPlacement().getUnit().getId(), x -> new ArrayList<>()).add(r);
        LinkedHashMap<Long, Unit> required = new LinkedHashMap<>();
        for (var p : placements) if (p.getUnitType() == UnitType.CORE) required.put(p.getUnit().getId(), p.getUnit());
        for (var r : results) {
            var p = r.getStudentUnitRegistration().getCourseUnitPlacement();
            required.putIfAbsent(p.getUnit().getId(), p.getUnit());
        }
        int passed = 0, failed = 0, missing = 0, missingResults = 0, earnedCredits = 0;
        List<Blocker> blockers = new ArrayList<>();
        for (Unit u : required.values()) {
            var attempts = byUnit.getOrDefault(u.getId(), List.of());
            if (attempts.isEmpty()) {
                missing++;
                blockers.add(new Blocker("MISSING_UNIT", u.getCode(), "The required unit was not registered"));
                continue;
            }
            var released = attempts.stream().filter(r -> r.getStatus() == ResultStatus.APPROVED || r.getStatus() == ResultStatus.RELEASED).toList();
            boolean unitPassed = released.stream().anyMatch(StudentResult::isPassed);
            if (unitPassed) {
                passed++;
                earnedCredits += u.getCreditHours() == null ? 0 : u.getCreditHours();
            } else if (released.isEmpty()) {
                missingResults++;
                blockers.add(new Blocker("MISSING_RESULT", u.getCode(), "No approved or released result is available"));
            } else {
                failed++;
                blockers.add(new Blocker("FAILED_UNIT", u.getCode(), "The unit has not been passed"));
            }
        }
        boolean finalPeriod = e.getCurrentCourseAcademicPeriod().isFinalPeriod() && e.getCurrentCourseAcademicPeriod().getNextPeriod() == null;
        if (!finalPeriod)
            blockers.add(new Blocker("FINAL_PERIOD", "", "The final course period has not been completed"));
        Integer requiredCredits = e.getCourse().getTotalCredits();
        if (requiredCredits == null)
            blockers.add(new Blocker("CONFIGURATION", "CREDITS", "Required course credits are not configured"));
        else if (earnedCredits < requiredCredits)
            blockers.add(new Blocker("INSUFFICIENT_CREDITS", "CREDITS", earnedCredits + " of " + requiredCredits + " credits attained"));
        var clearance = clearanceRepository.findByEnrollmentIdAndAcademicYearId(e.getId(), e.getCurrentAcademicYear().getId()).orElse(null);
        Set<Long> requiredDepartments = new HashSet<>();
        requiredDepartments.add(e.getCourse().getDepartment().getId());
        clearanceConfigRepository.findByAcademicYearIdOrderByDisplayOrderAsc(e.getCurrentAcademicYear().getId()).forEach(x -> requiredDepartments.add(x.getDepartment().getId()));
        Set<Long> cleared = new HashSet<>();
        if (clearance != null) for (ClearanceCheck c : clearance.getChecks())
            if (c.getStatus() == ClearanceCheckStatus.CLEARED) cleared.add(c.getDepartment().getId());
        boolean clearanceComplete = !requiredDepartments.isEmpty() && cleared.containsAll(requiredDepartments);
        if (!clearanceComplete) for (Long id : requiredDepartments)
            if (!cleared.contains(id))
                blockers.add(new Blocker("CLEARANCE", String.valueOf(id), "A required department has not cleared the student"));
        boolean eligible = blockers.isEmpty();
        return new GraduationReadinessResponse(eligible ? "ELIGIBLE" : requiredCredits == null ? "REQUIRES_REVIEW" : "NOT_ELIGIBLE", eligible, e.getCourse().getCode(), e.getCourse().getName(), finalPeriod, required.size(), passed, failed, missing, missingResults, requiredCredits, earnedCredits, clearanceComplete, requiredDepartments.size(), (int) requiredDepartments.stream().filter(cleared::contains).count(), LocalDateTime.now(), blockers);
    }
}
