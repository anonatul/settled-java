package com.settled.controller;

import com.settled.common.ApiResponse;
import com.settled.common.CurrentUser;
import com.settled.common.PageResponse;
import com.settled.dto.policy.PolicyResponse;
import com.settled.service.PolicyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/policies")
@RequiredArgsConstructor
@Tag(name = "Policies", description = "Customer policy management")
public class PolicyController {

    private final PolicyService policyService;

    @GetMapping
    @Operation(summary = "List my policies")
    public ApiResponse<PageResponse<PolicyResponse>> listMine(@CurrentUser UUID userId,
                                                              @PageableDefault(size = 10) Pageable pageable) {
        return ApiResponse.ok(policyService.listMine(userId, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one of my policies")
    public ApiResponse<PolicyResponse> getMine(@CurrentUser UUID userId, @PathVariable UUID id) {
        return ApiResponse.ok(policyService.getMine(userId, id));
    }
}