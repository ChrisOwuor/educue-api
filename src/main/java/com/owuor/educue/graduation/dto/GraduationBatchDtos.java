package com.owuor.educue.graduation.dto;
import jakarta.validation.constraints.*; import java.time.*; import java.util.UUID;
public final class GraduationBatchDtos { private GraduationBatchDtos(){}
 public record CreateRequest(@NotBlank String name,@NotNull LocalDate graduationDate,@NotNull UUID academicYearUuid,String search,Long courseId,Long departmentId){}
 public record Summary(UUID uuid,String name,LocalDate graduationDate,String academicYearCode,String status,int totalCandidates,int processedCandidates,int successfulCandidates,int failedCandidates,String createdBy,LocalDateTime createdAt,LocalDateTime conferredAt){}
 public record Candidate(Long id,Long applicationId,String admissionNumber,String studentName,String courseCode,String courseName,String departmentName,String status,String failureReason,LocalDateTime processedAt){}
 public record Detail(Summary batch){}
}
