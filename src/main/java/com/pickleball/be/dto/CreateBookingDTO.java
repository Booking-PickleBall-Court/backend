package com.pickleball.be.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CreateBookingDTO {
    private Long courtId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String notes;
    private String paymentMethod;
} 