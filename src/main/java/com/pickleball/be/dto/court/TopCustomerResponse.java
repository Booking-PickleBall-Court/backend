package com.pickleball.be.dto.court;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class TopCustomerResponse {
    private Long customerId;
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private Integer totalBookings;
    private Integer totalHoursBooked;
    private BigDecimal totalSpent;
    private LocalDateTime lastBookingDate;
} 