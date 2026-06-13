package com.baasid.ecommerce.service;

import com.baasid.ecommerce.dto.request.LoginRequest;
import com.baasid.ecommerce.dto.response.LoginResponse;

public interface AuthService {
    LoginResponse login(LoginRequest request);
}