package com.pickleball.be.dto.court;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.YearMonth;

@Data
@Builder
public class MonthlyRevenueResponse {
    private YearMonth month;
    private BigDecimal totalRevenue;
    private Integer totalHoursBooked;
    private Integer totalBookings;
} 