package com.settled.controller;

import com.settled.common.ApiResponse;
import com.settled.common.CurrentUser;
import com.settled.common.PageResponse;
import com.settled.domain.User;
import com.settled.domain.enums.Role;
import com.settled.domain.enums.UserStatus;
import com.settled.dto.admin.AnalyticsResponse;
import com.settled.dto.admin.AuditLogResponse;
import com.settled.dto.admin.RoleChangeRequest;
import com.settled.dto.claim.ClaimResponse;
import com.settled.dto.dashboard.OfficerDashboard;
import com.settled.dto.policy.PolicyRequest;
import com.settled.dto.policy.PolicyResponse;
import com.settled.dto.policy.PolicyTypeRequest;
import com.settled.dto.policy.PolicyTypeResponse;
import com.settled.dto.user.UserResponse;
import com.settled.dto.user.UserUpdateRequest;
import com.settled.exception.ResourceNotFoundException;
import com.settled.repository.UserRepository;
import com.settled.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Administrative operations")
public class AdminController {

    private final AdminService adminService;
    private final PolicyService policyService;
    private final PolicyTypeService policyTypeService;
    private final ClaimService claimService;
    private final AnalyticsService analyticsService;
    private final DashboardService dashboardService;
    private final UserRepository userRepository;

    @GetMapping("/users")
    @Operation(summary = "Search users with filters")
    public ApiResponse<PageResponse<UserResponse>> users(@RequestParam(required = false) String q,
                                                         @RequestParam(required = false) Role role,
                                                         @PageableDefault(size = 10) Pageable pageable) {
        return ApiResponse.ok(adminService.searchUsers(q, role, pageable));
    }

    @GetMapping("/users/{id}")
    @Operation(summary = "Get a user")
    public ApiResponse<UserResponse> user(@PathVariable UUID id) {
        return ApiResponse.ok(adminService.getUser(id));
    }

    @PutMapping("/users/{id}")
    @Operation(summary = "Update a user's profile details")
    public ApiResponse<UserResponse> updateUser(@PathVariable UUID id,
                                                @Valid @RequestBody UserUpdateRequest request) {
        return ApiResponse.ok(adminService.updateUser(id, request));
    }

    @PutMapping("/users/{id}/role")
    @Operation(summary = "Change a user's role")
    public ApiResponse<UserResponse> changeRole(@CurrentUser UUID adminId, @PathVariable UUID id,
                                                @Valid @RequestBody RoleChangeRequest request) {
        return ApiResponse.ok(adminService.changeRole(id, request, loadUser(adminId)));
    }

    @PutMapping("/users/{id}/status")
    @Operation(summary = "Activate or lock a user")
    public ApiResponse<UserResponse> setStatus(@CurrentUser UUID adminId, @PathVariable UUID id,
                                               @RequestParam UserStatus status) {
        return ApiResponse.ok(adminService.setUserStatus(id, status, loadUser(adminId)));
    }

    @GetMapping("/officers")
    @Operation(summary = "List claim officers")
    public ApiResponse<PageResponse<UserResponse>> officers(@RequestParam(required = false) String q,
                                                            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.ok(adminService.searchUsers(q, Role.CLAIM_OFFICER, pageable));
    }

    @PostMapping("/policies")
    @Operation(summary = "Create a policy for a customer")
    public ApiResponse<PolicyResponse> createPolicy(@CurrentUser UUID adminId,
                                                    @Valid @RequestBody PolicyRequest request,
                                                    @RequestParam UUID customerId) {
        return ApiResponse.ok(policyService.createForCustomer(customerId, request, loadUser(adminId)));
    }

    @GetMapping("/policies")
    @Operation(summary = "Search all policies")
    public ApiResponse<PageResponse<PolicyResponse>> policies(@RequestParam(required = false) String q,
                                                              @RequestParam(required = false) String status,
                                                              @PageableDefault(size = 10) Pageable pageable) {
        return ApiResponse.ok(policyService.search(q, status, pageable));
    }

    @GetMapping("/claims")
    @Operation(summary = "Search all claims")
    public ApiResponse<PageResponse<ClaimResponse>> claims(@RequestParam(required = false) String q,
                                                           @RequestParam(required = false) String status,
                                                           @PageableDefault(size = 10) Pageable pageable) {
        return ApiResponse.ok(claimService.listAll(q, status, pageable));
    }

    @GetMapping("/claims/{id}")
    @Operation(summary = "Get any claim")
    public ApiResponse<ClaimResponse> claim(@PathVariable UUID id) {
        return ApiResponse.ok(claimService.getAny(id));
    }

    @PostMapping("/policy-types")
    @Operation(summary = "Create a policy type")
    public ApiResponse<PolicyTypeResponse> createPolicyType(@Valid @RequestBody PolicyTypeRequest request) {
        return ApiResponse.ok(policyTypeService.create(request));
    }

    @PutMapping("/policy-types/{id}")
    @Operation(summary = "Update a policy type")
    public ApiResponse<PolicyTypeResponse> updatePolicyType(@PathVariable UUID id,
                                                            @Valid @RequestBody PolicyTypeRequest request) {
        return ApiResponse.ok(policyTypeService.update(id, request));
    }

    @GetMapping("/policy-types")
    @Operation(summary = "List all policy types including inactive")
    public ApiResponse<List<PolicyTypeResponse>> policyTypes() {
        return ApiResponse.ok(policyTypeService.listAll());
    }

    @GetMapping("/audit-logs")
    @Operation(summary = "Search audit logs")
    public ApiResponse<PageResponse<AuditLogResponse>> auditLogs(@RequestParam(required = false) String q,
                                                                 @RequestParam(required = false) String action,
                                                                 @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.ok(adminService.searchAuditLogs(q, action, pageable));
    }

    @GetMapping("/analytics")
    @Operation(summary = "Analytics summary (cached in Redis)")
    public ApiResponse<AnalyticsResponse> analytics() {
        return ApiResponse.ok(analyticsService.analytics());
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Admin dashboard summary (cached in Redis)")
    public ApiResponse<AnalyticsResponse> dashboard() {
        return ApiResponse.ok(analyticsService.analytics());
    }

    @GetMapping("/officer-dashboard/{officerId}")
    @Operation(summary = "Officer dashboard summary (cached in Redis)")
    public ApiResponse<OfficerDashboard> officerDashboard(@PathVariable UUID officerId) {
        return ApiResponse.ok(dashboardService.officerDashboard(officerId));
    }

    private User loadUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("User", userId));
    }
}