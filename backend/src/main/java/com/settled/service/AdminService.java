package com.settled.service;

import com.settled.common.PageResponse;
import com.settled.domain.User;
import com.settled.domain.enums.AuditAction;
import com.settled.domain.enums.Role;
import com.settled.domain.enums.UserStatus;
import com.settled.dto.admin.AuditLogResponse;
import com.settled.dto.admin.RoleChangeRequest;
import com.settled.dto.user.UserResponse;
import com.settled.dto.user.UserUpdateRequest;
import com.settled.exception.BadRequestException;
import com.settled.exception.ConflictException;
import com.settled.exception.ResourceNotFoundException;
import com.settled.repository.AuditLogRepository;
import com.settled.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> searchUsers(String q, Role role, Pageable pageable) {
        return PageResponse.from(userRepository.search(q, role, pageable), UserResponse::from);
    }

    @Transactional(readOnly = true)
    public UserResponse getUser(UUID userId) {
        return UserResponse.from(getUserEntity(userId));
    }

    @Transactional
    public UserResponse updateUser(UUID userId, UserUpdateRequest request) {
        User user = getUserEntity(userId);
        userRepository.findByEmailIgnoreCase(request.email())
                .filter(existing -> !existing.getId().equals(userId))
                .ifPresent(existing -> {
                    throw new ConflictException("Email already in use");
                });
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setPhone(request.phone());
        user.setEmail(request.email().trim().toLowerCase());
        return UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public UserResponse changeRole(UUID userId, RoleChangeRequest request, User admin) {
        User user = getUserEntity(userId);
        if (user.getId().equals(admin.getId())) {
            throw new BadRequestException("You cannot change your own role");
        }
        if (user.getRole() == Role.ADMIN && request.role() != Role.ADMIN) {
            long admins = userRepository.findByRole(Role.ADMIN).stream()
                    .filter(u -> u.getStatus() == UserStatus.ACTIVE)
                    .count();
            if (admins <= 1) {
                throw new BadRequestException("Cannot demote the last active admin");
            }
        }
        Role previous = user.getRole();
        user.setRole(request.role());
        User saved = userRepository.save(user);
        auditService.log(admin, AuditAction.USER_ROLE_CHANGED, "User", saved.getId(),
                "Role changed from " + previous + " to " + saved.getRole() + " for " + saved.getEmail(), null);
        return UserResponse.from(saved);
    }

    @Transactional
    public UserResponse setUserStatus(UUID userId, UserStatus status, User admin) {
        User user = getUserEntity(userId);
        if (user.getId().equals(admin.getId())) {
            throw new BadRequestException("You cannot change your own status");
        }
        user.setStatus(status);
        return UserResponse.from(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> searchAuditLogs(String q, String action, Pageable pageable) {
        AuditAction auditAction = null;
        if (action != null && !action.isBlank()) {
            try {
                auditAction = AuditAction.valueOf(action.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid audit action: " + action);
            }
        }
        return PageResponse.from(auditLogRepository.search(q, auditAction, pageable), AuditLogResponse::from);
    }

    private User getUserEntity(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("User", userId));
    }
}