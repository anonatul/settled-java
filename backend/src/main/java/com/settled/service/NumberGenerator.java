package com.settled.service;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class NumberGenerator {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    public String claimNumber() {
        return "CLM-" + DATE.format(LocalDate.now()) + "-" + randomDigits(6);
    }

    public String policyNumber() {
        return "POL-" + DATE.format(LocalDate.now()) + "-" + randomDigits(6);
    }

    public String settlementNumber() {
        return "STL-" + DATE.format(LocalDate.now()) + "-" + randomDigits(6);
    }

    public String customerNumber() {
        return "CUS-" + randomDigits(8);
    }

    private String randomDigits(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ThreadLocalRandom.current().nextInt(10));
        }
        return sb.toString();
    }
}