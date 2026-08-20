package com.settled.service;

import com.settled.domain.Customer;
import com.settled.domain.User;
import com.settled.domain.enums.Role;
import com.settled.dto.auth.AuthResponse;
import com.settled.dto.auth.LoginRequest;
import com.settled.dto.auth.RegisterRequest;
import com.settled.exception.BadRequestException;
import com.settled.repository.CustomerRepository;
import com.settled.repository.UserRepository;
import com.settled.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtService jwtService;
    @Mock private AuditService auditService;
    @Mock private NumberGenerator numberGenerator;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, customerRepository, passwordEncoder,
                authenticationManager, jwtService, auditService, numberGenerator);
        authService.expirationMs = 86400000L;
    }

    @Test
    void registerCreatesCustomerAndReturnsToken() {
        when(userRepository.existsByEmailIgnoreCase("new@test.io")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(numberGenerator.customerNumber()).thenReturn("CUS-00000001");
        when(customerRepository.existsByCustomerNumber(anyString())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtService.generateToken(any(), anyString(), anyString())).thenReturn("jwt-token");

        RegisterRequest request = new RegisterRequest("new@test.io", "password123", "Test", "User",
                "9876543210", LocalDate.of(1995, 1, 1), "Street 1", "Pune", "MH", "411001", "IN");
        AuthResponse response = authService.register(request, null);

        assertEquals("jwt-token", response.token());
        assertEquals(Role.CUSTOMER, response.user().role());
        assertNotNull(response.customer());
        verify(userRepository).save(any(User.class));
        verify(customerRepository).save(any(Customer.class));
        verify(auditService).log(any(User.class), any(), any(), any(), anyString(), any());
    }

    @Test
    void registerRejectsDuplicateEmail() {
        when(userRepository.existsByEmailIgnoreCase("dup@test.io")).thenReturn(true);
        RegisterRequest request = new RegisterRequest("dup@test.io", "password123", "A", "B",
                null, null, null, null, null, null, null);
        assertThrows(BadRequestException.class, () -> authService.register(request, null));
    }

    @Test
    void loginReturnsTokenForValidCredentials() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("user@test.io");
        user.setRole(Role.CUSTOMER);

        when(userRepository.findByEmailIgnoreCase("user@test.io")).thenReturn(Optional.of(user));
        when(customerRepository.findByUserId(user.getId())).thenReturn(Optional.empty());
        when(jwtService.generateToken(any(), anyString(), anyString())).thenReturn("jwt-token");

        AuthResponse response = authService.login(new LoginRequest("user@test.io", "password123"), null);

        assertEquals("jwt-token", response.token());
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void loginRejectsInvalidCredentials() {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new org.springframework.security.authentication.BadCredentialsException("bad"));
        assertThrows(BadRequestException.class,
                () -> authService.login(new LoginRequest("user@test.io", "wrong"), null));
    }
}