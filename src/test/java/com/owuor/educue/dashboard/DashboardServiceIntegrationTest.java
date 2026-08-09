package com.owuor.educue.dashboard;

import com.owuor.educue.dashboard.service.DashboardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class DashboardServiceIntegrationTest {
    @Autowired DashboardService dashboardService;

    @Test
    void dashboardQueriesReturnCompletePayloads() {
        var finance = dashboardService.finance();
        var admin = dashboardService.admin();

        assertThat(finance.monthlyCashFlow()).hasSize(12);
        assertThat(finance.monthlyCollections()).hasSize(12);
        assertThat(finance.yearlyCollections()).hasSize(7);
        assertThat(finance.chargePosition()).hasSize(2);
        assertThat(dashboardService.finance(2024, 2026, 6).monthlyCollections()).hasSize(6);
        assertThat(finance.collectedThisMonth()).isNotNull();
        assertThat(admin.monthlyGrowth()).hasSize(12);
        assertThat(admin.totalStudents()).isGreaterThanOrEqualTo(0);
    }
}
