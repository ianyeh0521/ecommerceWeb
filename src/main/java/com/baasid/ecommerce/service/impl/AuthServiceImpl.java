package com.baasid.ecommerce.service.impl;

import com.baasid.ecommerce.dto.request.LoginRequest;
import com.baasid.ecommerce.dto.response.LoginResponse;
import com.baasid.ecommerce.entity.User;
import com.baasid.ecommerce.exception.UnauthorizedException;
import com.baasid.ecommerce.repository.UserRepository;
import com.baasid.ecommerce.security.JwtUtil;
import com.baasid.ecommerce.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public LoginResponse login(LoginRequest request) {
        Optional<User> optionalUser = userRepository.findByUsername(request.getUsername());
        if (optionalUser.isEmpty()) {
            throw new UnauthorizedException("認證失敗");
        }

        User user = optionalUser.get();
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new UnauthorizedException("認證失敗");
        }

        String token = jwtUtil.generateToken(user);
        return new LoginResponse(token);
    }
}
