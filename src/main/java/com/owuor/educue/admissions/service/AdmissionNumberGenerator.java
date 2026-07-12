package com.owuor.educue.admissions.service;

import org.springframework.stereotype.Component;

import java.time.Year;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class AdmissionNumberGenerator {

    public String generate(String courseCode) {

        int studentCode =
                ThreadLocalRandom.current()
                        .nextInt(10000, 100000);

        int year = Year.now().getValue();

        return String.format(
                "%s/%05d/%d",
                courseCode.toUpperCase(),
                studentCode,
                year
        );
    }
}
