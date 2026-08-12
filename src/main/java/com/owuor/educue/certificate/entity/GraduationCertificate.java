package com.owuor.educue.certificate.entity;

import lombok.*;

import java.time.*;
import java.util.*;

@Getter
@Setter
@NoArgsConstructor
public class GraduationCertificate {
    private String certificateNumber;
    private String verificationCode = UUID.randomUUID().toString();
    private String studentName;
    private String admissionNumber;
    private String courseCode;
    private String courseName;
    private String awardTitle;
    private String awardClassification;
    private LocalDate graduationDate;
}
