package com.owuor.educue.results.service;

import com.owuor.educue.academics.enums.AttemptType;
import com.owuor.educue.academics.enums.RegistrationStatus;
import com.owuor.educue.finance.service.FeeLedgerService;
import com.owuor.educue.results.dto.RegisterAnotherAttemptResponse;
import com.owuor.educue.results.enums.ResultStatus;
import com.owuor.educue.results.repository.StudentResultRepository;
import com.owuor.educue.students.entity.StudentUnitRegistration;
import com.owuor.educue.students.enums.EnrollmentStatus;
import com.owuor.educue.students.repository.StudentUnitRegistrationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class StudentResultAttemptService {
    private final StudentResultRepository resultRepository;
    private final StudentUnitRegistrationRepository registrationRepository;
    private final FeeLedgerService feeLedgerService;

    @Value("${app.finance.attempt-fee-per-credit:1000}")
    private BigDecimal feePerCredit;

    @Transactional
    public RegisterAnotherAttemptResponse register(Long resultId, String requestedType, Long userId) {
        var result = resultRepository.findByIdForAttemptRegistration(resultId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Result not found"));
        var original = result.getStudentUnitRegistration();
        var enrollment = original.getEnrollment();
        if (!enrollment.getStudent().getUser().getId().equals(userId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This result belongs to another student");
        if (result.getStatus() != ResultStatus.APPROVED && result.getStatus() != ResultStatus.RELEASED)
            throw conflict("Only an approved or released result can be registered for another attempt");
        if (result.isPassed()) throw conflict("A passed unit cannot be registered for another attempt");
        if (resultRepository.existsPassedAttempt(enrollment.getId(), original.getCourseUnitPlacement().getId()))
            throw conflict("This unit has already been passed in a later attempt");
        if (enrollment.getStatus() != EnrollmentStatus.ACTIVE)
            throw conflict("Only active students can register another attempt");

        AttemptType type = switch (requestedType.toUpperCase()) {
            case "RESIT", "SUPPLEMENTARY" -> AttemptType.SUPPLEMENTARY;
            case "RETAKE" -> AttemptType.RETAKE;
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Attempt type must be RESIT or RETAKE");
        };
        if (registrationRepository.existsActiveFurtherAttempt(enrollment.getId(), original.getCourseUnitPlacement().getId()))
            throw conflict("This unit already has an active supplementary or retake registration");

        original.setStatus(RegistrationStatus.COMPLETED);
        registrationRepository.save(original);
        StudentUnitRegistration next = new StudentUnitRegistration();
        next.setEnrollment(enrollment);
        next.setCourseUnitPlacement(original.getCourseUnitPlacement());
        next.setAttemptType(type);
        next.setStatus(RegistrationStatus.ACTIVE);
        next = registrationRepository.save(next);

        int credits = original.getCourseUnitPlacement().getUnit().getCreditHours();
        BigDecimal charge = feePerCredit.multiply(BigDecimal.valueOf(credits));
        feeLedgerService.billUnitAttempt(enrollment, next, charge);
        return new RegisterAnotherAttemptResponse(next.getId(), label(type),
                "Unit registered for " + label(type).toLowerCase() + ". KES " + charge.toPlainString() + " has been debited (" + credits + " credits)." );
    }

    private String label(AttemptType type) { return type == AttemptType.SUPPLEMENTARY ? "RESIT" : "RETAKE"; }
    private ResponseStatusException conflict(String message) { return new ResponseStatusException(HttpStatus.CONFLICT, message); }
}
