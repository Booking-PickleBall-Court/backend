package com.pickleball.be.dto.user;

import com.pickleball.be.model.User;
import com.pickleball.be.model.UserRole;
import com.pickleball.be.model.UserStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UserManagementResponse {
    private Long id;
    private String fullName;
    private String email;
    private String phoneNumber;
    private String avatarUrl;
    private UserRole role;
    private UserStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static UserManagementResponse fromUser(User user) {
        return UserManagementResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
} 