package com.settled.controller;

import com.settled.common.ApiResponse;
import com.settled.common.CurrentUser;
import com.settled.domain.enums.Role;
import com.settled.repository.UserRepository;
import com.settled.dto.dashboard.CustomerDashboard;
import com.settled.dto.dashboard.OfficerDashboard;
import com.settled.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Dashboards", description = "Role-based dashboard summaries (cached in Redis)")
public class DashboardController {

    private final DashboardService dashboardService;
    private final UserRepository userRepository;

    @GetMapping("/dashboard")
    @Operation(summary = "Customer dashboard summary (cached 5 minutes)")
    public ApiResponse<CustomerDashboard> customerDashboard(@CurrentUser UUID userId) {
        return ApiResponse.ok(dashboardService.customerDashboard(userId));
    }

    @GetMapping("/officer/dashboard")
    @Operation(summary = "Officer dashboard summary (cached 2 minutes)")
    public ApiResponse<OfficerDashboard> officerDashboard(@CurrentUser UUID userId) {
        return ApiResponse.ok(dashboardService.officerDashboard(userId));
    }
}