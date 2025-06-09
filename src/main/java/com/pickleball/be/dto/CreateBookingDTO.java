package com.pickleball.be.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class CreateBookingDTO {
    private Long courtId;
    private List<Long> subCourtIds;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String notes;
    private String paymentMethod;
} 