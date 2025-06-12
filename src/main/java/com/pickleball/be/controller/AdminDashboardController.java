package com.pickleball.be.controller;

import com.pickleball.be.dto.admin.DashboardSummaryResponse;
import com.pickleball.be.dto.court.CourtRevenueResponse;
import com.pickleball.be.dto.court.MonthlyRevenueResponse;
import com.pickleball.be.dto.court.TopCustomerResponse;
import com.pickleball.be.service.BookingService;
import com.pickleball.be.service.CourtService;
import com.pickleball.be.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminDashboardController {
    private final CourtService courtService;
    private final UserService userService;
    private final BookingService bookingService;

    @GetMapping
    public DashboardSummaryResponse getDashboardSummary() {
        int courtCount = courtService.getAllCourts().size();
        int userCount = userService.getAllUsers().size();
        int activeCourtCount = courtService.getAvailableCourts().size();
        List<CourtRevenueResponse> courtRevenues = courtService.getAllCourtsRevenue();
        BigDecimal totalRevenue = courtRevenues.stream()
                .map(CourtRevenueResponse::getTotalRevenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        List<MonthlyRevenueResponse> monthlyRevenue = courtService.getAllCourtsMonthlyRevenue();
        List<CourtRevenueResponse> topCourts = courtRevenues.stream()
                .sorted(Comparator.comparing(CourtRevenueResponse::getTotalRevenue).reversed())
                .limit(5)
                .toList();
        List<TopCustomerResponse> topCustomers = getTopCustomersForAdmin();
        return DashboardSummaryResponse.builder()
                .totalRevenue(totalRevenue)
                .courtCount(courtCount)
                .userCount(userCount)
                .activeCourtCount(activeCourtCount)
                .monthlyRevenue(monthlyRevenue)
                .topCourts(topCourts)
                .topCustomers(topCustomers)
                .build();
    }

    private List<TopCustomerResponse> getTopCustomersForAdmin() {
        var allBookings = bookingService.getAllBookings().stream()
                .filter(b -> "PAID".equalsIgnoreCase(b.getPaymentStatus()))
                .toList();
        var bookingsByCustomer = allBookings.stream()
                .collect(java.util.stream.Collectors.groupingBy(b -> b.getUser()));
        return bookingsByCustomer.entrySet().stream()
                .map(entry -> {
                    var customer = entry.getKey();
                    var customerBookings = entry.getValue();
                    int totalBookings = customerBookings.size();
                    int totalMinutesBooked = customerBookings.stream()
                            .mapToInt(b -> (int) java.time.Duration.between(b.getStartTime(), b.getEndTime()).toMinutes())
                            .sum();
                    int totalHoursBooked = totalMinutesBooked / 60;
                    BigDecimal totalSpent = customerBookings.stream()
                            .map(b -> BigDecimal.valueOf(b.getTotalPrice()))
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    java.time.LocalDateTime lastBookingDate = customerBookings.stream()
                            .map(b -> b.getStartTime())
                            .max(java.time.LocalDateTime::compareTo)
                            .orElse(null);
                    return TopCustomerResponse.builder()
                            .customerId(customer.getId())
                            .customerName(customer.getFullName())
                            .customerEmail(customer.getEmail())
                            .customerPhone(customer.getPhoneNumber())
                            .totalBookings(totalBookings)
                            .totalHoursBooked(totalHoursBooked)
                            .totalSpent(totalSpent)
                            .lastBookingDate(lastBookingDate)
                            .build();
                })
                .sorted((c1, c2) -> c2.getTotalSpent().compareTo(c1.getTotalSpent()))
                .limit(5)
                .toList();
    }
} 