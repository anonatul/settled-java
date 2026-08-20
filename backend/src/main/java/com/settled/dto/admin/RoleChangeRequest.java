package com.settled.dto.admin;

import com.settled.domain.enums.Role;
import jakarta.validation.constraints.NotNull;

public record RoleChangeRequest(
        @NotNull(message = "Role is required")
        Role role
) {
}