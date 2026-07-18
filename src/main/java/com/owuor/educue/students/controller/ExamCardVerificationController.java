package com.owuor.educue.students.controller;

import com.owuor.educue.students.dto.ExamCardVerificationResponse;
import com.owuor.educue.students.service.ExamCardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/public/exam-cards")
@RequiredArgsConstructor
public class ExamCardVerificationController {
    private final ExamCardService examCardService;

    @GetMapping("/{verificationCode}")
    public ExamCardVerificationResponse verify(@PathVariable UUID verificationCode) {
        return examCardService.verify(verificationCode);
    }
}
