package com.settled.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final StringRedisTemplate redisTemplate;

    public boolean tryConsume(String key, int max, Duration window) {
        String current = redisTemplate.opsForValue().get(key);
        if (current == null) {
            redisTemplate.opsForValue().set(key, "1", window);
            return true;
        }
        long count = Long.parseLong(current);
        if (count >= max) {
            return false;
        }
        redisTemplate.opsForValue().increment(key);
        return true;
    }

    public long remaining(String key, int max) {
        String current = redisTemplate.opsForValue().get(key);
        if (current == null) {
            return max;
        }
        return Math.max(0, max - Long.parseLong(current));
    }
}