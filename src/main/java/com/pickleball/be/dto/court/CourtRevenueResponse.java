package com.pickleball.be.dto.court;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class CourtRevenueResponse {
    private Long courtId;
    private String courtName;
    private BigDecimal totalRevenue;
    private Integer totalBookings;
    private Integer totalHoursBooked;
} 