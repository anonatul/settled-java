package com.settled.service;

import com.settled.domain.Customer;
import com.settled.domain.User;
import com.settled.domain.enums.AuditAction;
import com.settled.domain.enums.Role;
import com.settled.dto.auth.AuthResponse;
import com.settled.dto.auth.LoginRequest;
import com.settled.dto.auth.RegisterRequest;
import com.settled.dto.customer.CustomerResponse;
import com.settled.dto.user.UserResponse;
import com.settled.exception.BadRequestException;
import com.settled.exception.ResourceNotFoundException;
import com.settled.repository.CustomerRepository;
import com.settled.repository.UserRepository;
import com.settled.security.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final AuditService auditService;
    private final NumberGenerator numberGenerator;

    @Value("${app.jwt.expiration-ms}")
    long expirationMs;

    @Transactional
    public AuthResponse register(RegisterRequest request, HttpServletRequest httpRequest) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new BadRequestException("An account with this email already exists");
        }

        User user = new User();
        user.setEmail(request.email().trim().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setPhone(request.phone());
        user.setRole(Role.CUSTOMER);
        userRepository.save(user);

        Customer customer = new Customer();
        customer.setUser(user);
        customer.setCustomerNumber(generateUniqueCustomerNumber());
        customer.setDateOfBirth(request.dateOfBirth() != null ? request.dateOfBirth() : LocalDate.of(1990, 1, 1));
        customer.setAddress(request.address());
        customer.setCity(request.city());
        customer.setState(request.state());
        customer.setPostalCode(request.postalCode());
        customer.setCountry(request.country() != null ? request.country() : "IN");
        customerRepository.save(customer);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                auditService.log(user, AuditAction.USER_REGISTERED, "User", user.getId(),
                        "New customer registered: " + user.getEmail(), clientIp(httpRequest));
            }
        });

        String token = jwtService.generateToken(user.getId(), user.getEmail(), user.getRole().name());
        return AuthResponse.of(token, expirationMs, UserResponse.from(user), CustomerResponse.from(customer));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email().trim(), request.password()));
            User user = userRepository.findByEmailIgnoreCase(request.email().trim())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            auditService.log(user, AuditAction.LOGIN, "User", user.getId(),
                    "Login from " + clientIp(httpRequest), clientIp(httpRequest));

            String token = jwtService.generateToken(user.getId(), user.getEmail(), user.getRole().name());
            Customer customer = customerRepository.findByUserId(user.getId()).orElse(null);
            return AuthResponse.of(token, expirationMs, UserResponse.from(user),
                    customer != null ? CustomerResponse.from(customer) : null);
        } catch (AuthenticationException e) {
            throw new BadRequestException("Invalid email or password");
        }
    }

    public UserResponse me(User user) {
        return UserResponse.from(user);
    }

    private String generateUniqueCustomerNumber() {
        String number;
        do {
            number = numberGenerator.customerNumber();
        } while (customerRepository.existsByCustomerNumber(number));
        return number;
    }

    private String clientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}