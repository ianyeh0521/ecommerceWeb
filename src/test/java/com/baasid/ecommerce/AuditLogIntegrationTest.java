package com.baasid.ecommerce;

import com.baasid.ecommerce.dto.request.LoginRequest;
import com.baasid.ecommerce.dto.request.ProductRequest;
import com.baasid.ecommerce.dto.response.LoginResponse;
import com.baasid.ecommerce.dto.response.ProductResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@Transactional(propagation = Propagation.NOT_SUPPORTED)
class AuditLogIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    private String adminToken;
    private String userToken;

    @BeforeEach
    void setUp() {
        adminToken = login("admin", "admin123");
        userToken = login("user1", "user123");
    }

    private String login(String username, String password) {
        LoginRequest req = new LoginRequest();
        req.setUsername(username);
        req.setPassword(password);
        ResponseEntity<LoginResponse> resp = restTemplate.postForEntity("/api/auth/login", req, LoginResponse.class);
        assertNotNull(resp.getBody());
        return resp.getBody().getToken();
    }

    private HttpHeaders bearerHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private ProductResponse createProduct(String name) {
        ProductRequest req = new ProductRequest();
        req.setName(name);
        req.setPrice(BigDecimal.valueOf(9.99));
        req.setStock(10);
        HttpEntity<ProductRequest> entity = new HttpEntity<>(req, bearerHeaders(adminToken));
        ResponseEntity<ProductResponse> resp = restTemplate.exchange(
                "/api/products", HttpMethod.POST, entity, ProductResponse.class);
        assertEquals(HttpStatus.CREATED, resp.getStatusCode());
        return resp.getBody();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getAuditLogContent(String query) {
        HttpEntity<Void> entity = new HttpEntity<>(bearerHeaders(adminToken));
        ResponseEntity<Map> resp = restTemplate.exchange(
                "/api/audit-logs" + query, HttpMethod.GET, entity, Map.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        return (List<Map<String, Object>>) resp.getBody().get("content");
    }

    @Test
    void getAuditLogs_adminNoFilter_returns200WithPage() {
        createProduct("FilterTestProduct");

        HttpEntity<Void> entity = new HttpEntity<>(bearerHeaders(adminToken));
        ResponseEntity<Map> resp = restTemplate.exchange("/api/audit-logs", HttpMethod.GET, entity, Map.class);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertNotNull(resp.getBody().get("content"));
    }

    @Test
    void getAuditLogs_adminFilterByProductId_returnsOnlyMatchingEntries() {
        ProductResponse target = createProduct("TargetProduct");
        createProduct("OtherProduct");

        List<Map<String, Object>> content = getAuditLogContent("?productId=" + target.getId());

        assertFalse(content.isEmpty());
        for (Map<String, Object> entry : content) {
            assertEquals(target.getId().intValue(), entry.get("productId"));
        }
    }

    @Test
    void getAuditLogs_adminFilterByOperationType_returnsOnlyMatchingEntries() {
        createProduct("OperationTypeProduct");

        List<Map<String, Object>> content = getAuditLogContent("?operationType=CREATE");

        assertFalse(content.isEmpty());
        for (Map<String, Object> entry : content) {
            assertEquals("CREATE", entry.get("operationType"));
        }
    }

    @Test
    void getAuditLogs_adminFilterByOperatedBy_returnsOnlyMatchingEntries() {
        createProduct("OperatedByProduct");

        List<Map<String, Object>> allContent = getAuditLogContent("");
        assertFalse(allContent.isEmpty());
        Integer adminId = (Integer) allContent.get(0).get("operatedBy");

        List<Map<String, Object>> filtered = getAuditLogContent("?operatedBy=" + adminId);

        assertFalse(filtered.isEmpty());
        for (Map<String, Object> entry : filtered) {
            assertEquals(adminId, entry.get("operatedBy"));
        }
    }

    @Test
    void getAuditLogs_adminFilterByOperatedBy_nonExistentId_returnsEmptyPage() {
        List<Map<String, Object>> content = getAuditLogContent("?operatedBy=999999");

        assertTrue(content.isEmpty());
    }

    @Test
    void getAuditLogs_adminFilterByDateRange_returnsEntriesWithinRange() {
        createProduct("DateRangeProduct");

        String from = LocalDateTime.now().minusMinutes(1).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String to = LocalDateTime.now().plusMinutes(1).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        List<Map<String, Object>> content = getAuditLogContent("?from=" + from + "&to=" + to);

        assertFalse(content.isEmpty());
    }

    @Test
    void getAuditLogs_adminFromAfterTo_returnsEmptyPage() {
        createProduct("ImpossibleRangeProduct");

        String from = LocalDateTime.now().plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String to = LocalDateTime.now().minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        List<Map<String, Object>> content = getAuditLogContent("?from=" + from + "&to=" + to);

        assertTrue(content.isEmpty());
    }

    @Test
    void getAuditLogs_adminCombinesMultipleFilters_returnsOnlyMatchingEntries() {
        ProductResponse target = createProduct("CombinedFilterProduct");

        List<Map<String, Object>> all = getAuditLogContent("");
        assertFalse(all.isEmpty());
        Integer adminId = (Integer) all.get(0).get("operatedBy");

        String from = LocalDateTime.now().minusMinutes(1).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        List<Map<String, Object>> content = getAuditLogContent(
                "?productId=" + target.getId() + "&operatedBy=" + adminId + "&operationType=CREATE&from=" + from);

        assertFalse(content.isEmpty());
        for (Map<String, Object> entry : content) {
            assertEquals(target.getId().intValue(), entry.get("productId"));
            assertEquals(adminId, entry.get("operatedBy"));
            assertEquals("CREATE", entry.get("operationType"));
        }
    }

    @Test
    void getAuditLogs_userRole_returns403() {
        HttpEntity<Void> entity = new HttpEntity<>(bearerHeaders(userToken));
        ResponseEntity<Void> resp = restTemplate.exchange("/api/audit-logs", HttpMethod.GET, entity, Void.class);

        assertEquals(HttpStatus.FORBIDDEN, resp.getStatusCode());
    }
}
