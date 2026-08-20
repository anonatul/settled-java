package com.settled.controller;

import com.settled.common.ApiResponse;
import com.settled.dto.policy.PolicyTypeResponse;
import com.settled.service.PolicyTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/policy-types")
@RequiredArgsConstructor
@Tag(name = "Policy Types", description = "Policy type catalog")
public class PolicyTypeController {

    private final PolicyTypeService policyTypeService;

    @GetMapping
    @Operation(summary = "List active policy types (cached in Redis)")
    public ApiResponse<List<PolicyTypeResponse>> list() {
        return ApiResponse.ok(policyTypeService.listActive());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a policy type by id")
    public ApiResponse<PolicyTypeResponse> get(@PathVariable UUID id) {
        return ApiResponse.ok(policyTypeService.get(id));
    }
}