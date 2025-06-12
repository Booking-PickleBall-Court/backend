package com.pickleball.be.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class JwtResponse {
    private String token;
    private String type = "Bearer";
    private Long id;
    private String email;
    private String fullName;
    private String role;
    private String avatarUrl;
    private String phoneNumber;

    public JwtResponse(String token, Long id, String email, String fullName, String role, String avatarUrl, String phoneNumber) {
        this.token = token;
        this.id = id;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
        this.avatarUrl = avatarUrl;
        this.phoneNumber = phoneNumber;
    }
} 