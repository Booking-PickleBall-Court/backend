package com.pickleball.be.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;

@Configuration
public class CloudinaryConfig {
    @Bean
    public Cloudinary getCloudinary(){
        Map config = new HashMap();
        config.put("cloud_name", "");
        config.put("api_key", "");
        config.put("api_secret", "");
        config.put("secure", true);
        return new Cloudinary(config);
    }
} 