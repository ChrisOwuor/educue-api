package com.owuor.educue.clearance.dto;
import jakarta.validation.constraints.*; import java.time.*; import java.util.*;
public final class ClearanceDtos { private ClearanceDtos(){}
 public record CycleRequest(@NotBlank String name,@NotNull UUID academicYearUuid,@NotNull LocalDate opensOn,@NotNull LocalDate closesOn,boolean active){}
 public record DeadlineWindow(UUID uuid,String academicYearCode,LocalDateTime startsAt,LocalDateTime deadlineAt,boolean active){}
 public record YearDepartmentsRequest(@NotEmpty List<Long> departmentIds){}
 public record DepartmentItem(Long id,String name,Integer displayOrder,boolean mandatory){}
 public record Preview(boolean visible,boolean eligible,String message,DeadlineWindow window,String courseCode,String courseName,List<DepartmentItem> departments){}
 public record CheckItem(Long id,Long departmentId,String departmentName,String status,String remarks,String reviewedBy,LocalDateTime reviewedAt){}
 public record ApplyRequest(@NotEmpty List<Long> departmentIds){}
 public record ApplicationResponse(UUID uuid,String status,String admissionNumber,String studentName,String courseCode,String courseName,DeadlineWindow window,LocalDateTime appliedAt,LocalDateTime completedAt,List<CheckItem> checks){}
 public record ReviewRequest(@NotNull String status,String remarks){}
 public record OfficerRequest(Long checkId,UUID applicationUuid,String admissionNumber,String studentName,String courseCode,String courseName,String academicYearCode,String departmentName,String status,String remarks,LocalDateTime appliedAt){}
}
