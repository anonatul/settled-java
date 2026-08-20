package com.settled.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.settled.common.ApiResponse;
import com.settled.service.RateLimitService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Component
@RequiredArgsConstructor
public class LoginRateLimitFilter extends OncePerRequestFilter {

    private static final String KEY_PREFIX = "rl:login:";

    private final RateLimitService rateLimitService;
    private final ObjectMapper objectMapper;

    @Value("${app.rate-limit.login-max:5}")
    private int loginMax;

    @Value("${app.rate-limit.login-window-seconds:60}")
    private long windowSeconds;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getMethod().equalsIgnoreCase("POST")
                || !request.getRequestURI().endsWith("/api/v1/auth/login");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String key = KEY_PREFIX + clientIp(request);
        if (!rateLimitService.tryConsume(key, loginMax, Duration.ofSeconds(windowSeconds))) {
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            objectMapper.writeValue(response.getWriter(),
                    ApiResponse.error("Too many login attempts. Please try again in " + windowSeconds + " seconds"));
            return;
        }
        filterChain.doFilter(request, response);
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}