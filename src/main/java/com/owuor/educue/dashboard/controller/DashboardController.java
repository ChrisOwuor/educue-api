package com.owuor.educue.dashboard.controller;

import com.owuor.educue.dashboard.dto.DashboardDtos.AdminDashboard;
import com.owuor.educue.dashboard.dto.DashboardDtos.FinanceDashboard;
import com.owuor.educue.dashboard.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboards")
@RequiredArgsConstructor
public class DashboardController {
    private final DashboardService dashboardService;

    @GetMapping("/finance")
    @PreAuthorize("hasAnyRole('FINANCE','ADMIN')")
    public FinanceDashboard finance(
            @RequestParam(required = false) Integer fromYear,
            @RequestParam(required = false) Integer toYear,
            @RequestParam(defaultValue = "12") Integer months) {
        return dashboardService.finance(fromYear, toYear, months);
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public AdminDashboard admin() { return dashboardService.admin(); }
}
