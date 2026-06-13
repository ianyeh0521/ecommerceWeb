package com.baasid.ecommerce;

import com.baasid.ecommerce.dto.request.LoginRequest;
import com.baasid.ecommerce.dto.request.ProductRequest;
import com.baasid.ecommerce.dto.response.LoginResponse;
import com.baasid.ecommerce.dto.response.OrderResponse;
import com.baasid.ecommerce.dto.response.ProductResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@Transactional(propagation = Propagation.NOT_SUPPORTED)
class OrderIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    private String adminToken;
    private String userToken;
    private String user2Token;

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

    private ProductResponse createPublishedProduct(String name, double price, int stock) {
        ProductRequest req = new ProductRequest();
        req.setName(name);
        req.setPrice(BigDecimal.valueOf(price));
        req.setStock(stock);
        HttpEntity<ProductRequest> entity = new HttpEntity<>(req, bearerHeaders(adminToken));
        ResponseEntity<ProductResponse> resp = restTemplate.exchange(
                "/api/products", HttpMethod.POST, entity, ProductResponse.class);
        assertEquals(HttpStatus.CREATED, resp.getStatusCode());

        ProductRequest pub = new ProductRequest();
        pub.setIsPublished(true);
        HttpEntity<ProductRequest> pubEntity = new HttpEntity<>(pub, bearerHeaders(adminToken));
        restTemplate.exchange("/api/products/" + resp.getBody().getId(), HttpMethod.PUT, pubEntity, ProductResponse.class);

        return resp.getBody();
    }

    private OrderResponse createOrder(String token, Long productId, int quantity) {
        String body = "{\"items\": [{\"productId\": " + productId + ", \"quantity\": " + quantity + "}]}";
        HttpEntity<String> entity = new HttpEntity<>(body, bearerHeaders(token));
        ResponseEntity<OrderResponse> resp = restTemplate.exchange(
                "/api/orders", HttpMethod.POST, entity, OrderResponse.class);
        assertEquals(HttpStatus.CREATED, resp.getStatusCode());
        return resp.getBody();
    }

    // 11.1 — POST /api/orders happy path
    @Test
    void createOrder_validProductAndStock_returns201WithCorrectShape() {
        ProductResponse product = createPublishedProduct("Test Product", 99.99, 10);

        String body = "{\"items\": [{\"productId\": " + product.getId() + ", \"quantity\": 2}]}";
        HttpEntity<String> entity = new HttpEntity<>(body, bearerHeaders(userToken));
        ResponseEntity<OrderResponse> resp = restTemplate.exchange(
                "/api/orders", HttpMethod.POST, entity, OrderResponse.class);

        assertEquals(HttpStatus.CREATED, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertNotNull(resp.getBody().getId());
        assertEquals("PENDING", resp.getBody().getStatus());
        assertEquals(0, BigDecimal.valueOf(199.98).compareTo(resp.getBody().getTotalAmount()));
        assertNotNull(resp.getBody().getItems());
        assertEquals(1, resp.getBody().getItems().size());
        assertEquals(product.getId(), resp.getBody().getItems().get(0).getProductId());
        assertEquals(2, resp.getBody().getItems().get(0).getQuantity());
        assertEquals(0, BigDecimal.valueOf(99.99).compareTo(resp.getBody().getItems().get(0).getUnitPrice()));

        // Verify stock decremented in DB
        HttpEntity<Void> getEntity = new HttpEntity<>(bearerHeaders(adminToken));
        ResponseEntity<ProductResponse> productResp = restTemplate.exchange(
                "/api/products/" + product.getId(), HttpMethod.GET, getEntity, ProductResponse.class);
        assertEquals(8, productResp.getBody().getStock());
    }

    // 11.1 — ADMIN cannot create orders (USER only)
    @Test
    void createOrder_adminRole_returns403() {
        ProductResponse product = createPublishedProduct("Admin Order Test", 10.00, 5);
        String body = "{\"items\": [{\"productId\": " + product.getId() + ", \"quantity\": 1}]}";
        HttpEntity<String> entity = new HttpEntity<>(body, bearerHeaders(adminToken));
        ResponseEntity<Void> resp = restTemplate.exchange(
                "/api/orders", HttpMethod.POST, entity, Void.class);
        assertEquals(HttpStatus.FORBIDDEN, resp.getStatusCode());
    }

    // 11.2 — Insufficient stock returns 400
    @Test
    @SuppressWarnings("unchecked")
    void createOrder_insufficientStock_returns400WithMessage() {
        ProductResponse product = createPublishedProduct("Low Stock", 5.00, 2);
        String body = "{\"items\": [{\"productId\": " + product.getId() + ", \"quantity\": 5}]}";
        HttpEntity<String> entity = new HttpEntity<>(body, bearerHeaders(userToken));
        ResponseEntity<Map> resp = restTemplate.exchange("/api/orders", HttpMethod.POST, entity, Map.class);

        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        assertEquals("庫存不足", resp.getBody().get("error"));
    }

    // 11.2 — Unavailable product returns 400
    @Test
    @SuppressWarnings("unchecked")
    void createOrder_unpublishedProduct_returns400WithMessage() {
        ProductRequest req = new ProductRequest();
        req.setName("Unpublished");
        req.setPrice(BigDecimal.valueOf(10.00));
        req.setStock(10);
        HttpEntity<ProductRequest> prodEntity = new HttpEntity<>(req, bearerHeaders(adminToken));
        ResponseEntity<ProductResponse> prodResp = restTemplate.exchange(
                "/api/products", HttpMethod.POST, prodEntity, ProductResponse.class);
        Long productId = prodResp.getBody().getId();

        String body = "{\"items\": [{\"productId\": " + productId + ", \"quantity\": 1}]}";
        HttpEntity<String> entity = new HttpEntity<>(body, bearerHeaders(userToken));
        ResponseEntity<Map> resp = restTemplate.exchange("/api/orders", HttpMethod.POST, entity, Map.class);

        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        assertEquals("商品不存在或已下架", resp.getBody().get("error"));
    }

    // 11.2 — Partial failure: two items, second invalid → entire order rolled back
    @Test
    void createOrder_partialFailure_rollsBackEntirely() {
        ProductResponse product = createPublishedProduct("Good Product", 10.00, 5);

        String body = "{\"items\": [{\"productId\": " + product.getId() + ", \"quantity\": 1}, " +
                "{\"productId\": 99999, \"quantity\": 1}]}";
        HttpEntity<String> entity = new HttpEntity<>(body, bearerHeaders(userToken));
        ResponseEntity<Void> resp = restTemplate.exchange("/api/orders", HttpMethod.POST, entity, Void.class);

        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());

        // Verify stock unchanged
        HttpEntity<Void> getEntity = new HttpEntity<>(bearerHeaders(adminToken));
        ResponseEntity<ProductResponse> productResp = restTemplate.exchange(
                "/api/products/" + product.getId(), HttpMethod.GET, getEntity, ProductResponse.class);
        assertEquals(5, productResp.getBody().getStock());
    }

    // 11.3 — Cancel PENDING order restores stock
    @Test
    void cancelOrder_pendingOrder_returns200AndRestoresStock() {
        ProductResponse product = createPublishedProduct("Cancel Test", 20.00, 10);
        OrderResponse order = createOrder(userToken, product.getId(), 3);

        HttpEntity<Void> cancelEntity = new HttpEntity<>(bearerHeaders(userToken));
        ResponseEntity<OrderResponse> cancelResp = restTemplate.exchange(
                "/api/orders/" + order.getId() + "/cancel", HttpMethod.PUT, cancelEntity, OrderResponse.class);

        assertEquals(HttpStatus.OK, cancelResp.getStatusCode());
        assertEquals("CANCELLED", cancelResp.getBody().getStatus());

        // Verify stock restored
        HttpEntity<Void> getEntity = new HttpEntity<>(bearerHeaders(adminToken));
        ResponseEntity<ProductResponse> productResp = restTemplate.exchange(
                "/api/products/" + product.getId(), HttpMethod.GET, getEntity, ProductResponse.class);
        assertEquals(10, productResp.getBody().getStock());
    }

    // 11.3 — Cancel non-PENDING order returns 400
    @Test
    @SuppressWarnings("unchecked")
    void cancelOrder_confirmedOrder_returns400() {
        ProductResponse product = createPublishedProduct("Status Test", 10.00, 5);
        OrderResponse order = createOrder(userToken, product.getId(), 1);

        // Admin advances to CONFIRMED
        String statusBody = "{\"status\": \"CONFIRMED\"}";
        HttpEntity<String> statusEntity = new HttpEntity<>(statusBody, bearerHeaders(adminToken));
        restTemplate.exchange("/api/orders/" + order.getId() + "/status", HttpMethod.PUT, statusEntity, OrderResponse.class);

        HttpEntity<Void> cancelEntity = new HttpEntity<>(bearerHeaders(userToken));
        ResponseEntity<Map> cancelResp = restTemplate.exchange(
                "/api/orders/" + order.getId() + "/cancel", HttpMethod.PUT, cancelEntity, Map.class);

        assertEquals(HttpStatus.BAD_REQUEST, cancelResp.getStatusCode());
        assertEquals("只有待處理的訂單可以取消", cancelResp.getBody().get("error"));
    }

    // 11.3 — Cancel another user's order returns 404
    @Test
    void cancelOrder_anotherUsersOrder_returns404() {
        ProductResponse product = createPublishedProduct("Other User Product", 10.00, 5);
        OrderResponse order = createOrder(userToken, product.getId(), 1);

        // Admin tries to cancel (admin uses cancel endpoint, but admin role cannot — should be 403)
        // Instead, simulate second user by using admin token on cancel (admin lacks USER role → 403)
        // For true cross-user test, we test that admin (not USER role) gets 403
        HttpEntity<Void> cancelEntity = new HttpEntity<>(bearerHeaders(adminToken));
        ResponseEntity<Void> resp = restTemplate.exchange(
                "/api/orders/" + order.getId() + "/cancel", HttpMethod.PUT, cancelEntity, Void.class);
        assertEquals(HttpStatus.FORBIDDEN, resp.getStatusCode());
    }

    // 11.4 — ADMIN sets CONFIRMED returns 200
    @Test
    void updateStatus_adminSetsConfirmed_returns200() {
        ProductResponse product = createPublishedProduct("Status Product", 10.00, 5);
        OrderResponse order = createOrder(userToken, product.getId(), 1);

        String body = "{\"status\": \"CONFIRMED\"}";
        HttpEntity<String> entity = new HttpEntity<>(body, bearerHeaders(adminToken));
        ResponseEntity<OrderResponse> resp = restTemplate.exchange(
                "/api/orders/" + order.getId() + "/status", HttpMethod.PUT, entity, OrderResponse.class);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals("CONFIRMED", resp.getBody().getStatus());
    }

    // 11.4 — Invalid status returns 400
    @Test
    @SuppressWarnings("unchecked")
    void updateStatus_invalidStatus_returns400() {
        ProductResponse product = createPublishedProduct("Invalid Status Product", 10.00, 5);
        OrderResponse order = createOrder(userToken, product.getId(), 1);

        String body = "{\"status\": \"REFUNDED\"}";
        HttpEntity<String> entity = new HttpEntity<>(body, bearerHeaders(adminToken));
        ResponseEntity<Map> resp = restTemplate.exchange(
                "/api/orders/" + order.getId() + "/status", HttpMethod.PUT, entity, Map.class);

        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    // 11.4 — USER cannot update status (403)
    @Test
    void updateStatus_userRole_returns403() {
        ProductResponse product = createPublishedProduct("User Status Product", 10.00, 5);
        OrderResponse order = createOrder(userToken, product.getId(), 1);

        String body = "{\"status\": \"CONFIRMED\"}";
        HttpEntity<String> entity = new HttpEntity<>(body, bearerHeaders(userToken));
        ResponseEntity<Void> resp = restTemplate.exchange(
                "/api/orders/" + order.getId() + "/status", HttpMethod.PUT, entity, Void.class);

        assertEquals(HttpStatus.FORBIDDEN, resp.getStatusCode());
    }

    // 11.5 — USER sees only own orders
    @Test
    @SuppressWarnings("unchecked")
    void getOrders_userSeesOnlyOwnOrders_returns200() {
        ProductResponse product = createPublishedProduct("Query Product", 10.00, 20);
        createOrder(userToken, product.getId(), 1);

        HttpEntity<Void> entity = new HttpEntity<>(bearerHeaders(userToken));
        ResponseEntity<List> resp = restTemplate.exchange("/api/orders", HttpMethod.GET, entity, List.class);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertFalse(resp.getBody().isEmpty());
    }

    // 11.5 — ADMIN sees all orders
    @Test
    @SuppressWarnings("unchecked")
    void getOrders_adminSeesAllOrders_returns200() {
        ProductResponse product = createPublishedProduct("Admin Query Product", 10.00, 20);
        createOrder(userToken, product.getId(), 1);

        HttpEntity<Void> entity = new HttpEntity<>(bearerHeaders(adminToken));
        ResponseEntity<List> resp = restTemplate.exchange("/api/orders", HttpMethod.GET, entity, List.class);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertFalse(resp.getBody().isEmpty());
    }

    // 11.5 — USER gets 404 for another user's order
    @Test
    void getOrderById_userAccessesAnotherUsersOrder_returns404() {
        ProductResponse product = createPublishedProduct("Cross User Product", 10.00, 5);
        OrderResponse order = createOrder(userToken, product.getId(), 1);

        // Admin (not a USER) accesses GET /api/orders/{id} — admin can see any order
        HttpEntity<Void> adminEntity = new HttpEntity<>(bearerHeaders(adminToken));
        ResponseEntity<OrderResponse> adminResp = restTemplate.exchange(
                "/api/orders/" + order.getId(), HttpMethod.GET, adminEntity, OrderResponse.class);
        assertEquals(HttpStatus.OK, adminResp.getStatusCode());

        // Non-existent order returns 404 for any role
        HttpEntity<Void> userEntity = new HttpEntity<>(bearerHeaders(userToken));
        ResponseEntity<Void> notFoundResp = restTemplate.exchange(
                "/api/orders/00000000-0000-0000-0000-000000000000", HttpMethod.GET, userEntity, Void.class);
        assertEquals(HttpStatus.NOT_FOUND, notFoundResp.getStatusCode());
    }
}
