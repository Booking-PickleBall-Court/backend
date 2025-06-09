package com.pickleball.be.dto.court;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class OwnerRevenueResponse {
    private Long ownerId;
    private String ownerName;
    private BigDecimal totalRevenue;
    private Integer totalCourts;
    private Integer totalBookings;
    private Integer totalHoursBooked;
} 