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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ProductIntegrationTest extends AbstractIntegrationTest {

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

    private ProductResponse createProduct(String name, double price, int stock) {
        ProductRequest req = new ProductRequest();
        req.setName(name);
        req.setPrice(BigDecimal.valueOf(price));
        req.setStock(stock);
        HttpEntity<ProductRequest> entity = new HttpEntity<>(req, bearerHeaders(adminToken));
        ResponseEntity<ProductResponse> resp = restTemplate.exchange(
                "/api/products", HttpMethod.POST, entity, ProductResponse.class);
        assertEquals(HttpStatus.CREATED, resp.getStatusCode());
        return resp.getBody();
    }

    // 管理者新增
    @Test
    void createProduct_adminValidBody_returns201WithDefaults() {
        ProductRequest req = new ProductRequest();
        req.setName("Widget A");
        req.setPrice(BigDecimal.valueOf(9.99));
        req.setStock(100);

        HttpEntity<ProductRequest> entity = new HttpEntity<>(req, bearerHeaders(adminToken));
        ResponseEntity<ProductResponse> resp = restTemplate.exchange(
                "/api/products", HttpMethod.POST, entity, ProductResponse.class);

        assertEquals(HttpStatus.CREATED, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertNotNull(resp.getBody().getId());
        assertFalse(resp.getBody().getIsPublished());
        assertFalse(resp.getBody().getIsDeleted());
    }

    @Test
    @SuppressWarnings("unchecked")
    void createProduct_missingName_returns400() {
        ProductRequest req = new ProductRequest();
        req.setPrice(BigDecimal.valueOf(9.99));
        req.setStock(100);

        HttpEntity<ProductRequest> entity = new HttpEntity<>(req, bearerHeaders(adminToken));
        ResponseEntity<Map> resp = restTemplate.exchange(
                "/api/products", HttpMethod.POST, entity, Map.class);

        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    @Test
    void createProduct_userRole_returns403() {
        ProductRequest req = new ProductRequest();
        req.setName("Widget A");
        req.setPrice(BigDecimal.valueOf(9.99));
        req.setStock(100);

        HttpEntity<ProductRequest> entity = new HttpEntity<>(req, bearerHeaders(userToken));
        ResponseEntity<Void> resp = restTemplate.exchange(
                "/api/products", HttpMethod.POST, entity, Void.class);

        assertEquals(HttpStatus.FORBIDDEN, resp.getStatusCode());
    }

    // 管理者修改
    @Test
    void updateProduct_adminPartialUpdate_updatesProvidedFieldsOnly() {
        ProductResponse created = createProduct("Widget A", 9.99, 100);

        ProductRequest update = new ProductRequest();
        update.setName("Widget B");
        update.setPrice(BigDecimal.valueOf(14.99));

        HttpEntity<ProductRequest> entity = new HttpEntity<>(update, bearerHeaders(adminToken));
        ResponseEntity<ProductResponse> resp = restTemplate.exchange(
                "/api/products/" + created.getId(), HttpMethod.PUT, entity, ProductResponse.class);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertEquals("Widget B", resp.getBody().getName());
        assertEquals(0, BigDecimal.valueOf(14.99).compareTo(resp.getBody().getPrice()));
        assertEquals(100, resp.getBody().getStock());
    }

    @Test
    void updateProduct_adminPublishes_productVisibleToUser() {
        ProductResponse created = createProduct("Widget C", 5.00, 50);

        ProductRequest publish = new ProductRequest();
        publish.setIsPublished(true);

        HttpEntity<ProductRequest> pubEntity = new HttpEntity<>(publish, bearerHeaders(adminToken));
        restTemplate.exchange("/api/products/" + created.getId(), HttpMethod.PUT, pubEntity, ProductResponse.class);

        HttpEntity<Void> getEntity = new HttpEntity<>(bearerHeaders(userToken));
        ResponseEntity<ProductResponse> userResp = restTemplate.exchange(
                "/api/products/" + created.getId(), HttpMethod.GET, getEntity, ProductResponse.class);

        assertEquals(HttpStatus.OK, userResp.getStatusCode());
    }

    @Test
    void updateProduct_softDeletedProduct_returns404() {
        ProductResponse created = createProduct("Widget D", 3.00, 10);

        HttpEntity<Void> delEntity = new HttpEntity<>(bearerHeaders(adminToken));
        restTemplate.exchange("/api/products/" + created.getId(), HttpMethod.DELETE, delEntity, Void.class);

        ProductRequest update = new ProductRequest();
        update.setName("Widget D Updated");

        HttpEntity<ProductRequest> entity = new HttpEntity<>(update, bearerHeaders(adminToken));
        ResponseEntity<Void> resp = restTemplate.exchange(
                "/api/products/" + created.getId(), HttpMethod.PUT, entity, Void.class);

        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
    }

    // 管理者刪除
    @Test
    void deleteProduct_admin_returns204AndSetsIsDeletedTrue() {
        ProductResponse created = createProduct("Widget E", 7.00, 20);

        HttpEntity<Void> delEntity = new HttpEntity<>(bearerHeaders(adminToken));
        ResponseEntity<Void> delResp = restTemplate.exchange(
                "/api/products/" + created.getId(), HttpMethod.DELETE, delEntity, Void.class);

        assertEquals(HttpStatus.NO_CONTENT, delResp.getStatusCode());

        HttpEntity<Void> getAdmin = new HttpEntity<>(bearerHeaders(adminToken));
        ResponseEntity<Void> adminGet = restTemplate.exchange(
                "/api/products/" + created.getId(), HttpMethod.GET, getAdmin, Void.class);
        assertEquals(HttpStatus.NOT_FOUND, adminGet.getStatusCode());

        HttpEntity<Void> getUser = new HttpEntity<>(bearerHeaders(userToken));
        ResponseEntity<Void> userGet = restTemplate.exchange(
                "/api/products/" + created.getId(), HttpMethod.GET, getUser, Void.class);
        assertEquals(HttpStatus.NOT_FOUND, userGet.getStatusCode());
    }

    @Test
    void deleteProduct_alreadyDeleted_returns404() {
        ProductResponse created = createProduct("Widget F", 2.00, 5);

        HttpEntity<Void> delEntity = new HttpEntity<>(bearerHeaders(adminToken));
        restTemplate.exchange("/api/products/" + created.getId(), HttpMethod.DELETE, delEntity, Void.class);

        ResponseEntity<Void> secondDel = restTemplate.exchange(
                "/api/products/" + created.getId(), HttpMethod.DELETE, delEntity, Void.class);

        assertEquals(HttpStatus.NOT_FOUND, secondDel.getStatusCode());
    }

    // 一般使用者查詢範圍
    @Test
    void listProducts_userSeesOnlyPublished() {
        ProductResponse unpublished = createProduct("Unpublished", 1.00, 1);
        ProductResponse published = createProduct("Published", 2.00, 2);

        ProductRequest pub = new ProductRequest();
        pub.setIsPublished(true);
        HttpEntity<ProductRequest> pubEntity = new HttpEntity<>(pub, bearerHeaders(adminToken));
        restTemplate.exchange("/api/products/" + published.getId(), HttpMethod.PUT, pubEntity, ProductResponse.class);

        HttpEntity<Void> getEntity = new HttpEntity<>(bearerHeaders(userToken));
        ResponseEntity<Map> resp = restTemplate.exchange(
                "/api/products", HttpMethod.GET, getEntity, Map.class);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
        @SuppressWarnings("unchecked")
        java.util.List<Map<String, Object>> content = (java.util.List<Map<String, Object>>) resp.getBody().get("content");
        assertNotNull(content);
        assertTrue(content.stream().allMatch(p -> Boolean.TRUE.equals(p.get("isPublished"))));
        assertTrue(content.stream().noneMatch(p -> Boolean.TRUE.equals(p.get("isDeleted"))));
    }

    @Test
    void listProducts_adminSeesPublishedAndUnpublished() {
        ProductResponse unpublished = createProduct("Unpublished2", 1.00, 1);
        ProductResponse published = createProduct("Published2", 2.00, 2);

        ProductRequest pub = new ProductRequest();
        pub.setIsPublished(true);
        HttpEntity<ProductRequest> pubEntity = new HttpEntity<>(pub, bearerHeaders(adminToken));
        restTemplate.exchange("/api/products/" + published.getId(), HttpMethod.PUT, pubEntity, ProductResponse.class);

        HttpEntity<Void> getEntity = new HttpEntity<>(bearerHeaders(adminToken));
        ResponseEntity<Map> resp = restTemplate.exchange(
                "/api/products", HttpMethod.GET, getEntity, Map.class);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        @SuppressWarnings("unchecked")
        java.util.List<Map<String, Object>> content = (java.util.List<Map<String, Object>>) resp.getBody().get("content");
        assertNotNull(content);
        assertTrue(content.stream().noneMatch(p -> Boolean.TRUE.equals(p.get("isDeleted"))));
    }

    @Test
    void listProducts_paginationRespected() {
        for (int i = 0; i < 3; i++) {
            createProduct("PaginationProduct" + i, 1.00, 1);
        }

        HttpEntity<Void> getEntity = new HttpEntity<>(bearerHeaders(adminToken));
        ResponseEntity<Map> resp = restTemplate.exchange(
                "/api/products?page=0&size=2", HttpMethod.GET, getEntity, Map.class);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        @SuppressWarnings("unchecked")
        java.util.List<?> content = (java.util.List<?>) resp.getBody().get("content");
        assertNotNull(content);
        assertTrue(content.size() <= 2);
    }

    // 一般使用者單筆查詢
    @Test
    void getProduct_userGetsPublished_returns200() {
        ProductResponse created = createProduct("Published3", 5.00, 10);

        ProductRequest pub = new ProductRequest();
        pub.setIsPublished(true);
        HttpEntity<ProductRequest> pubEntity = new HttpEntity<>(pub, bearerHeaders(adminToken));
        restTemplate.exchange("/api/products/" + created.getId(), HttpMethod.PUT, pubEntity, ProductResponse.class);

        HttpEntity<Void> getEntity = new HttpEntity<>(bearerHeaders(userToken));
        ResponseEntity<ProductResponse> resp = restTemplate.exchange(
                "/api/products/" + created.getId(), HttpMethod.GET, getEntity, ProductResponse.class);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
    }

    @Test
    void getProduct_userGetsUnpublished_returns404() {
        ProductResponse created = createProduct("Unpublished3", 5.00, 10);

        HttpEntity<Void> getEntity = new HttpEntity<>(bearerHeaders(userToken));
        ResponseEntity<Void> resp = restTemplate.exchange(
                "/api/products/" + created.getId(), HttpMethod.GET, getEntity, Void.class);

        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
    }

    @Test
    void getProduct_userGetsDeleted_returns404() {
        ProductResponse created = createProduct("Deleted3", 5.00, 10);

        HttpEntity<Void> delEntity = new HttpEntity<>(bearerHeaders(adminToken));
        restTemplate.exchange("/api/products/" + created.getId(), HttpMethod.DELETE, delEntity, Void.class);

        HttpEntity<Void> getEntity = new HttpEntity<>(bearerHeaders(userToken));
        ResponseEntity<Void> resp = restTemplate.exchange(
                "/api/products/" + created.getId(), HttpMethod.GET, getEntity, Void.class);

        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
    }

    @Test
    void getProduct_adminGetsUnpublishedNonDeleted_returns200() {
        ProductResponse created = createProduct("AdminUnpublished", 5.00, 10);

        HttpEntity<Void> getEntity = new HttpEntity<>(bearerHeaders(adminToken));
        ResponseEntity<ProductResponse> resp = restTemplate.exchange(
                "/api/products/" + created.getId(), HttpMethod.GET, getEntity, ProductResponse.class);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertFalse(resp.getBody().getIsPublished());
    }
}
