package com.owuor.educue.clearance.controller;
import com.owuor.educue.clearance.dto.ClearanceDtos.*; import com.owuor.educue.clearance.service.ClearanceService; import com.owuor.educue.users.entity.User;
import jakarta.validation.Valid; import lombok.RequiredArgsConstructor; import org.springframework.http.HttpStatus; import org.springframework.security.access.prepost.PreAuthorize; import org.springframework.security.core.annotation.AuthenticationPrincipal; import org.springframework.web.bind.annotation.*; import java.util.*;
import org.springframework.data.domain.*;
@RestController @RequestMapping("/api/clearance") @RequiredArgsConstructor
public class ClearanceController {
 private final ClearanceService service;
 @GetMapping("/academic-years/{yearUuid}/departments") @PreAuthorize("hasRole('ADMIN')") public List<DepartmentItem> departments(@PathVariable UUID yearUuid){return service.yearDepartments(yearUuid);}
 @PutMapping("/academic-years/{yearUuid}/departments") @PreAuthorize("hasRole('ADMIN')") public List<DepartmentItem> configure(@PathVariable UUID yearUuid,@Valid @RequestBody YearDepartmentsRequest request){return service.configureYear(yearUuid,request);}
 @GetMapping("/me/preview") @PreAuthorize("hasRole('STUDENT')") public Preview preview(@AuthenticationPrincipal User user){return service.preview(user);}
 @GetMapping("/me/application") @PreAuthorize("hasRole('STUDENT')") public ApplicationResponse mine(@AuthenticationPrincipal User user){return service.mine(user);}
 @PostMapping("/me/applications") @PreAuthorize("hasRole('STUDENT')") public ApplicationResponse apply(@AuthenticationPrincipal User user,@Valid @RequestBody ApplyRequest request){return service.apply(user,request);}
 @GetMapping("/officer/requests") @PreAuthorize("hasAnyRole('HOD','FINANCE','ADMIN')") public Page<OfficerRequest> requests(@AuthenticationPrincipal User user,@RequestParam(required=false) String search,@RequestParam(required=false) Long courseId,@RequestParam(required=false) Long departmentId,@RequestParam(required=false) String status,Pageable pageable){return service.officerRequests(user,search,courseId,departmentId,status,pageable);}
 @PatchMapping("/officer/checks/{checkId}") @PreAuthorize("hasAnyRole('HOD','FINANCE','ADMIN')") public ApplicationResponse review(@PathVariable Long checkId,@Valid @RequestBody ReviewRequest request,@AuthenticationPrincipal User user){return service.review(checkId,request,user);}
}
