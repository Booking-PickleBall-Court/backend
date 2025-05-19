package com.pickleball.be.service;

import com.pickleball.be.dto.auth.LoginRequest;
import com.pickleball.be.dto.auth.RegisterRequest;
import com.pickleball.be.model.User;

public interface UserService {
    User registerUser(RegisterRequest registerRequest);
    String authenticateUser(LoginRequest loginRequest);
    User getCurrentUser();
    User updateUser(User user);
    void deleteUser(Long userId);
    User getUserById(Long userId);
    boolean existsByEmail(String email);
} 