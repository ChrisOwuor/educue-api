package com.owuor.educue.results.service;

import com.owuor.educue.results.entity.StudentResult;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class GradingService {

    public void calculate(StudentResult result) {

        if (result.getTotalMarks() == null) {
            result.applyGrade(null, false);
            return;
        }

        BigDecimal total = result.getTotalMarks();

        String grade;
        boolean passed;

        if (total.compareTo(BigDecimal.valueOf(70)) >= 0) {
            grade = "A";
            passed = true;

        } else if (total.compareTo(BigDecimal.valueOf(60)) >= 0) {
            grade = "B";

            passed = true;

        } else if (total.compareTo(BigDecimal.valueOf(50)) >= 0) {
            grade = "C";

            passed = true;

        } else if (total.compareTo(BigDecimal.valueOf(40)) >= 0) {
            grade = "D";

            passed = true;

        } else {

            grade = "E";

            passed = false;
        }

        result.applyGrade(grade, passed);
    }
}
