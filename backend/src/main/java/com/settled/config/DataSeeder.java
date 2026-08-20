package com.settled.config;

import com.settled.domain.*;
import com.settled.domain.enums.ClaimStatus;
import com.settled.domain.enums.PolicyStatus;
import com.settled.domain.enums.Role;
import com.settled.domain.enums.UserStatus;
import com.settled.repository.*;
import com.settled.service.NumberGenerator;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Configuration
@RequiredArgsConstructor
public class DataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final PolicyTypeRepository policyTypeRepository;
    private final PolicyRepository policyRepository;
    private final ClaimRepository claimRepository;
    private final ClaimStatusHistoryRepository historyRepository;
    private final ClaimAssignmentRepository assignmentRepository;
    private final SettlementRepository settlementRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    CommandLineRunner seedData(NumberGenerator numbers) {
        return args -> {
            if (userRepository.count() > 0) {
                log.info("Database already seeded, skipping");
                return;
            }
            log.info("Seeding demo data...");

            User admin = createUser("admin@settled.io", "Admin", "User", Role.ADMIN);
            User officer1 = createUser("officer1@settled.io", "Riya", "Sharma", Role.CLAIM_OFFICER);
            User officer2 = createUser("officer2@settled.io", "Vikram", "Patel", Role.CLAIM_OFFICER);

            Customer customer1 = createCustomer("customer1@settled.io", "Aarav", "Mehta", numbers);
            Customer customer2 = createCustomer("customer2@settled.io", "Ananya", "Iyer", numbers);
            Customer customer3 = createCustomer("customer3@settled.io", "Kabir", "Singh", numbers);

            PolicyType health = createPolicyType("HEALTH", "Health Insurance",
                    "Comprehensive health coverage for hospitalization and treatment",
                    new BigDecimal("500000.00"), new BigDecimal("0.0350"));
            PolicyType auto = createPolicyType("AUTO", "Auto Insurance",
                    "Coverage for vehicle damage, theft and third-party liability",
                    new BigDecimal("200000.00"), new BigDecimal("0.0250"));
            PolicyType home = createPolicyType("HOME", "Home Insurance",
                    "Coverage for home structure and contents against fire, theft and natural disasters",
                    new BigDecimal("1000000.00"), new BigDecimal("0.0040"));

            Policy p1 = createPolicy(customer1, health, "POL-HLTH-001", new BigDecimal("17500.00"),
                    new BigDecimal("500000.00"), LocalDate.now().minusMonths(18), LocalDate.now().plusMonths(6));
            Policy p2 = createPolicy(customer1, auto, "POL-AUTO-001", new BigDecimal("5000.00"),
                    new BigDecimal("200000.00"), LocalDate.now().minusMonths(10), LocalDate.now().plusMonths(14));
            Policy p3 = createPolicy(customer2, home, "POL-HOME-001", new BigDecimal("4000.00"),
                    new BigDecimal("1000000.00"), LocalDate.now().minusMonths(24), LocalDate.now().minusMonths(1));
            Policy p4 = createPolicy(customer3, health, "POL-HLTH-002", new BigDecimal("17500.00"),
                    new BigDecimal("500000.00"), LocalDate.now().minusMonths(6), LocalDate.now().plusMonths(18));

            createClaim(customer1, p1, "CLM-SEED-001", ClaimStatus.SUBMITTED,
                    "Hospitalization", "Admitted for appendicitis surgery, bills attached",
                    new BigDecimal("85000.00"), null, null, officer1);
            createClaim(customer1, p2, "CLM-SEED-002", ClaimStatus.UNDER_REVIEW,
                    "Vehicle Accident", "Rear-end collision on highway, repair estimate attached",
                    new BigDecimal("45000.00"), null, null, officer1);
            createClaim(customer2, p3, "CLM-SEED-003", ClaimStatus.APPROVED,
                    "Water Damage", "Pipe burst caused damage to living room ceiling and furniture",
                    new BigDecimal("120000.00"), new BigDecimal("105000.00"), null, officer2);
            createClaim(customer3, p4, "CLM-SEED-004", ClaimStatus.ADDITIONAL_INFO_REQUIRED,
                    "Critical Illness", "Diagnosed with dengue, treatment ongoing",
                    new BigDecimal("96000.00"), null, null, officer2);
            createClaim(customer1, p1, "CLM-SEED-005", ClaimStatus.REJECTED,
                    "Outpatient Treatment", "Claim for routine outpatient consultation not covered under policy",
                    new BigDecimal("2500.00"), null, null, officer1);
            createClaim(customer1, p2, "CLM-SEED-006", ClaimStatus.SETTLED,
                    "Windshield Crack", "Windshield cracked by flying debris, replaced",
                    new BigDecimal("18000.00"), new BigDecimal("18000.00"), new BigDecimal("18000.00"), officer1);

            log.info("Demo data seeded: admin@settled.io / officer1@settled.io / officer2@settled.io / customer1@settled.io (password: password123)");
        };
    }

    private User createUser(String email, String firstName, String lastName, Role role) {
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode("password123"));
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        return userRepository.save(user);
    }

    private Customer createCustomer(String email, String firstName, String lastName, NumberGenerator numbers) {
        User user = createUser(email, firstName, lastName, Role.CUSTOMER);
        Customer customer = new Customer();
        customer.setUser(user);
        customer.setCustomerNumber(numbers.customerNumber());
        customer.setDateOfBirth(LocalDate.of(1992, 6, 15));
        customer.setAddress("42 Green Park Road");
        customer.setCity("Pune");
        customer.setState("Maharashtra");
        customer.setPostalCode("411001");
        customer.setCountry("IN");
        return customerRepository.save(customer);
    }

    private PolicyType createPolicyType(String code, String name, String description,
                                        BigDecimal coverage, BigDecimal rate) {
        PolicyType type = new PolicyType();
        type.setCode(code);
        type.setName(name);
        type.setDescription(description);
        type.setCoverageAmount(coverage);
        type.setPremiumRate(rate);
        type.setActive(true);
        return policyTypeRepository.save(type);
    }

    private Policy createPolicy(Customer customer, PolicyType type, String number,
                                BigDecimal premium, BigDecimal sumInsured,
                                LocalDate start, LocalDate end) {
        Policy policy = new Policy();
        policy.setCustomer(customer);
        policy.setPolicyType(type);
        policy.setPolicyNumber(number);
        policy.setStatus(start.isAfter(LocalDate.now().minusDays(1)) ? PolicyStatus.ACTIVE
                : end.isBefore(LocalDate.now()) ? PolicyStatus.EXPIRED : PolicyStatus.ACTIVE);
        policy.setStartDate(start);
        policy.setEndDate(end);
        policy.setPremium(premium);
        policy.setSumInsured(sumInsured);
        return policyRepository.save(policy);
    }

    private void createClaim(Customer customer, Policy policy, String number, ClaimStatus status,
                             String incidentType, String description,
                             BigDecimal requested, BigDecimal approved, BigDecimal settled,
                             User officer) {
        Claim claim = new Claim();
        claim.setCustomer(customer);
        claim.setPolicy(policy);
        claim.setClaimNumber(number);
        claim.setStatus(status);
        claim.setIncidentDate(LocalDate.now().minusDays(20));
        claim.setIncidentType(incidentType);
        claim.setDescription(description);
        claim.setAmountRequested(requested);
        claim.setAmountApproved(approved);
        claim.setSubmittedAt(Instant.now().minusSeconds(10 * 86400L));
        if (status == ClaimStatus.APPROVED || status == ClaimStatus.SETTLED) {
            claim.setDecidedAt(Instant.now().minusSeconds(4 * 86400L));
        }
        if (status == ClaimStatus.SETTLED) {
            claim.setSettledAt(Instant.now().minusSeconds(2 * 86400L));
        }
        Claim saved = claimRepository.save(claim);

        ClaimStatusHistory history = new ClaimStatusHistory();
        history.setClaim(saved);
        history.setFromStatus(null);
        history.setToStatus(status);
        history.setChangedBy(officer);
        history.setNote("Seeded demo claim");
        history.setChangedAt(saved.getSubmittedAt());
        historyRepository.save(history);

        ClaimAssignment assignment = new ClaimAssignment();
        assignment.setClaim(saved);
        assignment.setOfficer(officer);
        assignment.setAssignedBy(userRepository.findByEmailIgnoreCase("admin@settled.io").orElseThrow());
        assignment.setAssignedAt(saved.getSubmittedAt());
        assignment.setActive(true);
        assignmentRepository.save(assignment);

        if (status == ClaimStatus.SETTLED && settled != null) {
            Settlement settlement = new Settlement();
            settlement.setClaim(saved);
            settlement.setSettlementNumber("STL-SEED-" + saved.getClaimNumber().substring(5));
            settlement.setApprovedAmount(approved);
            settlement.setSettledAmount(settled);
            settlement.setSettlementDate(LocalDate.now().minusDays(2));
            settlement.setPaymentReference("PAY-REF-" + saved.getClaimNumber().substring(5));
            settlement.setProcessedBy(officer);
            settlement.setCreatedAt(saved.getSettledAt());
            settlementRepository.save(settlement);
        }
    }
}