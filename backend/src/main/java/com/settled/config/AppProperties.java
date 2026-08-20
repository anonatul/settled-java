package com.settled.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        String uploadDir,
        RateLimit rateLimit
) {
    public record RateLimit(int loginMax, long loginWindowSeconds, int claimMax, long claimWindowMinutes) {
    }
}