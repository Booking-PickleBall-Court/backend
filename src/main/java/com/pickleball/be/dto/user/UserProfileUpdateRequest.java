package com.pickleball.be.dto.user;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class UserProfileUpdateRequest {
    private String fullName;
    private String phoneNumber;
    private MultipartFile avatar; // optional
    private String currentPassword; // optional, chỉ dùng khi đổi password
    private String newPassword;     // optional, chỉ dùng khi đổi password
} 