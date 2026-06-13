package com.baasid.ecommerce.auth;

import com.baasid.ecommerce.AbstractIntegrationTest;
import com.baasid.ecommerce.dto.request.LoginRequest;
import com.baasid.ecommerce.dto.response.LoginResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@Transactional(propagation = Propagation.NOT_SUPPORTED)
class AuthIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void login_seededAdminUser_returns200WithToken() {
        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("admin123");

        ResponseEntity<LoginResponse> response = restTemplate.postForEntity(
                "/api/auth/login", request, LoginResponse.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getToken());
        assertFalse(response.getBody().getToken().isBlank());
    }

    @Test
    void login_seededRegularUser_returns200WithToken() {
        LoginRequest request = new LoginRequest();
        request.setUsername("user1");
        request.setPassword("user123");

        ResponseEntity<LoginResponse> response = restTemplate.postForEntity(
                "/api/auth/login", request, LoginResponse.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getToken());
        assertFalse(response.getBody().getToken().isBlank());
    }

    @Test
    @SuppressWarnings("unchecked")
    void login_wrongPassword_returns401WithErrorMessage() {
        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("wrongpassword");

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/auth/login", request, Map.class);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("帳號或密碼錯誤", response.getBody().get("error"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void login_unknownUsername_returns401WithErrorMessage() {
        LoginRequest request = new LoginRequest();
        request.setUsername("nonexistent");
        request.setPassword("password123");

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/auth/login", request, Map.class);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("帳號或密碼錯誤", response.getBody().get("error"));
    }
}
