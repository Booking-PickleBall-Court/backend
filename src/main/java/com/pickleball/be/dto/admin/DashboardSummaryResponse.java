package com.pickleball.be.dto.admin;

import com.pickleball.be.dto.court.CourtRevenueResponse;
import com.pickleball.be.dto.court.MonthlyRevenueResponse;
import com.pickleball.be.dto.court.TopCustomerResponse;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class DashboardSummaryResponse {
    private BigDecimal totalRevenue;
    private int courtCount;
    private int userCount;
    private int activeCourtCount;
    private List<MonthlyRevenueResponse> monthlyRevenue;
    private List<CourtRevenueResponse> topCourts;
    private List<TopCustomerResponse> topCustomers;
} 