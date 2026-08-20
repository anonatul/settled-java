package com.settled;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.settled.domain.enums.ClaimStatus;
import com.settled.repository.ClaimRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ClaimLifecycleIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("settled-test")
            .withUsername("test")
            .withPassword("test");

    @Container
    static final GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        registry.add("app.jwt.secret", () -> "test-secret-key-that-is-long-enough-for-hs256-signing");
        registry.add("app.upload-dir", () -> "target/test-uploads");
        registry.add("app.rate-limit.login-max", () -> 100);
        registry.add("app.rate-limit.claim-max", () -> 100);
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ClaimRepository claimRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private static final Map<String, String> SEED = Map.of(
            "customer1@settled.io", "password123",
            "officer1@settled.io", "password123",
            "admin@settled.io", "password123"
    );

    @Test
    void fullLifecycleFromRegisterToSettlement() throws Exception {
        String customerEmail = "it-customer@test.io";
        String registerBody = """
                {
                  "email": "%s",
                  "password": "password123",
                  "firstName": "IT",
                  "lastName": "Customer",
                  "phone": "9876543210",
                  "dateOfBirth": "1990-05-05"
                }
                """.formatted(customerEmail);
        ResponseEntity<String> register = restTemplate.exchange("/api/v1/auth/register",
                HttpMethod.POST, jsonBody(registerBody), String.class);
        assertEquals(HttpStatus.CREATED, register.getStatusCode());
        String customerToken = tokenFrom(register.getBody());

        ResponseEntity<String> me = restTemplate.exchange("/api/v1/auth/me", HttpMethod.GET,
                auth(customerToken), String.class);
        assertEquals(HttpStatus.OK, me.getStatusCode());

        String customerId = json(me.getBody()).path("data").path("id").asText();
        JsonNode customerProfile = json(get("/api/v1/customers/me", customerToken).getBody()).path("data");
        assertNotNull(customerProfile.path("customerNumber").asText());
        assertFalse(customerProfile.path("customerNumber").asText().isBlank());

        String adminToken = login("admin@settled.io");
        String officerToken = login("officer1@settled.io");

        JsonNode policyTypes = json(get("/api/v1/policy-types", customerToken).getBody()).path("data");
        String policyTypeId = null;
        for (JsonNode type : policyTypes) {
            if (type.path("coverageAmount").asDouble() >= 500000) {
                policyTypeId = type.path("id").asText();
                break;
            }
        }
        assertNotNull(policyTypeId, "No policy type with coverage >= 500000");

        String policyBody = """
                {
                  "policyTypeId": "%s",
                  "startDate": "%s",
                  "endDate": "2027-01-01",
                  "premium": 12000,
                  "sumInsured": 500000
                }
                """.formatted(policyTypeId, LocalDate.now());
        ResponseEntity<String> policyResp = restTemplate.exchange(
                "/api/v1/admin/policies?customerId=" + customerId,
                HttpMethod.POST, auth(adminToken, policyBody), String.class);
        assertEquals(HttpStatus.OK, policyResp.getStatusCode());
        String policyId = json(policyResp.getBody()).path("data").path("id").asText();

        String claimBody = """
                {
                  "policyId": "%s",
                  "incidentDate": "2026-08-01",
                  "incidentType": "Fire Damage",
                  "description": "Kitchen fire damaged property",
                  "amountRequested": 250000
                }
                """.formatted(policyId);
        ResponseEntity<String> claimResp = post("/api/v1/claims", customerToken, claimBody);
        assertEquals(HttpStatus.OK, claimResp.getStatusCode());
        JsonNode claim = json(claimResp.getBody()).path("data");
        String claimId = claim.path("id").asText();
        assertEquals("SUBMITTED", claim.path("status").asText());

        String officerId = json(get("/api/v1/admin/users?role=CLAIM_OFFICER&size=5", adminToken).getBody())
                .path("data").path("content").get(0).path("id").asText();
        ResponseEntity<String> assign = post("/api/v1/claims/" + claimId + "/assign", adminToken,
                "{\"officerId\":\"" + officerId + "\"}");
        assertEquals(HttpStatus.OK, assign.getStatusCode());
        assertEquals("UNDER_REVIEW", json(assign.getBody()).path("data").path("status").asText());

        ResponseEntity<String> approve = post("/api/v1/claims/" + claimId + "/approve", officerToken,
                "{\"amountApproved\":220000,\"note\":\"Verified by officer\"}");
        assertEquals(HttpStatus.OK, approve.getStatusCode());
        assertEquals("APPROVED", json(approve.getBody()).path("data").path("status").asText());

        ResponseEntity<String> settle = post("/api/v1/claims/" + claimId + "/settle", officerToken,
                "{\"settledAmount\":220000,\"paymentReference\":\"NEFT-IT-001\",\"settlementDate\":\"2026-08-20\"}");
        assertEquals(HttpStatus.OK, settle.getStatusCode());
        assertEquals("SETTLED", json(settle.getBody()).path("data").path("status").asText());

        ResponseEntity<String> settlement = get("/api/v1/claims/" + claimId + "/settlement", customerToken);
        JsonNode settlementData = json(settlement.getBody()).path("data");
        assertEquals(new BigDecimal("220000").doubleValue(), settlementData.path("settledAmount").asDouble());
        assertEquals("NEFT-IT-001", settlementData.path("paymentReference").asText());

        ResponseEntity<String> history = get("/api/v1/claims/" + claimId + "/history", customerToken);
        JsonNode statuses = json(history.getBody()).path("data");
        assertEquals(4, statuses.size());

        ResponseEntity<String> audit = get("/api/v1/admin/audit-logs?action=CLAIM_SETTLED", adminToken);
        assertEquals(HttpStatus.OK, audit.getStatusCode());
        assertTrue(json(audit.getBody()).path("data").path("totalElements").asLong() >= 1);

        ResponseEntity<String> analytics = get("/api/v1/admin/analytics", adminToken);
        assertEquals(HttpStatus.OK, analytics.getStatusCode());
        assertTrue(json(analytics.getBody()).path("data").path("totalClaims").asLong() >= 1);

        assertEquals(ClaimStatus.SETTLED, claimRepository.findById(java.util.UUID.fromString(claimId)).orElseThrow().getStatus());
    }

    @Test
    void securityDeniesCustomerAdminAccess() throws Exception {
        String customerToken = login("customer1@settled.io");
        ResponseEntity<String> response = get("/api/v1/admin/users", customerToken);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void invalidTransitionReturns409() throws Exception {
        String customerToken = login("customer1@settled.io");
        String adminToken = login("admin@settled.io");
        String officerToken = login("officer1@settled.io");

        String policyId = json(get("/api/v1/policies", customerToken).getBody())
                .path("data").path("content").get(0).path("id").asText();
        String claimBody = """
                {
                  "policyId": "%s",
                  "incidentDate": "2026-08-01",
                  "incidentType": "Theft",
                  "description": "Stolen phone",
                  "amountRequested": 30000
                }
                """.formatted(policyId);
        String claimId = json(post("/api/v1/claims", customerToken, claimBody).getBody()).path("data").path("id").asText();

        ResponseEntity<String> reject = post("/api/v1/claims/" + claimId + "/reject", officerToken, "{\"reason\":\"no\"}");
        assertEquals(HttpStatus.NOT_FOUND, reject.getStatusCode());

        String officerId = json(get("/api/v1/admin/users?role=CLAIM_OFFICER&size=5", adminToken).getBody())
                .path("data").path("content").get(0).path("id").asText();
        post("/api/v1/claims/" + claimId + "/assign", adminToken, "{\"officerId\":\"" + officerId + "\"}");

        ResponseEntity<String> approve = post("/api/v1/claims/" + claimId + "/approve", officerToken,
                "{\"amountApproved\":28000,\"note\":\"ok\"}");
        assertEquals(HttpStatus.OK, approve.getStatusCode());

        ResponseEntity<String> doubleApprove = post("/api/v1/claims/" + claimId + "/approve", officerToken,
                "{\"amountApproved\":28000,\"note\":\"again\"}");
        assertEquals(HttpStatus.CONFLICT, doubleApprove.getStatusCode());
    }

    private String login(String email) throws Exception {
        String body = "{\"email\":\"" + email + "\",\"password\":\"" + SEED.get(email) + "\"}";
        ResponseEntity<String> response = restTemplate.exchange("/api/v1/auth/login",
                HttpMethod.POST, jsonBody(body), String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        return tokenFrom(response.getBody());
    }

    private String tokenFrom(String body) throws Exception {
        return json(body).path("data").path("token").asText();
    }

    private JsonNode json(String body) throws Exception {
        return objectMapper.readTree(body);
    }

    private HttpEntity<String> auth(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return new HttpEntity<>(headers);
    }

    private HttpEntity<String> auth(String token, String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    private HttpEntity<String> jsonBody(String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    private ResponseEntity<String> get(String url, String token) {
        return restTemplate.exchange(url, HttpMethod.GET, auth(token), String.class);
    }

    private ResponseEntity<String> post(String url, String token, String body) {
        return restTemplate.exchange(url, HttpMethod.POST, auth(token, body), String.class);
    }
}