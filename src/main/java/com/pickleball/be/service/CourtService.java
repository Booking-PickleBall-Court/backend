package com.pickleball.be.service;

import com.pickleball.be.dto.court.CourtRequest;
import com.pickleball.be.dto.court.CourtRevenueResponse;
import com.pickleball.be.dto.court.MonthlyRevenueResponse;
import com.pickleball.be.dto.court.OwnerRevenueResponse;
import com.pickleball.be.dto.court.TopCustomerResponse;
import com.pickleball.be.model.Court;
import com.pickleball.be.model.CourtStatus;
import com.pickleball.be.model.CourtType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface CourtService {
    Court createCourt(CourtRequest request);
    Court updateCourt(Long id, CourtRequest request);
    void deleteCourt(Long id);
    Court getCourtById(Long id);
    List<Court> getAllCourts();
    List<Court> getCourtsByOwner(Long ownerId);
    List<Court> getAvailableCourts();
    List<Court> getCourtsByMaxPrice(BigDecimal maxPrice);
    Page<Court> searchCourts(BigDecimal minPrice, BigDecimal maxPrice, String address,
                           CourtType courtType, CourtStatus status, LocalDate date, Pageable pageable);
    CourtRevenueResponse getCourtRevenue(Long courtId);
    List<CourtRevenueResponse> getAllCourtsRevenue();
    OwnerRevenueResponse getOwnerRevenue(Long ownerId);
    List<OwnerRevenueResponse> getAllOwnersRevenue();
    List<MonthlyRevenueResponse> getMonthlyRevenue(Long courtId);
    List<MonthlyRevenueResponse> getOwnerMonthlyRevenue(Long ownerId);
    List<MonthlyRevenueResponse> getAllCourtsMonthlyRevenue();
    List<TopCustomerResponse> getTopCustomers(Long ownerId);
    
    // New method for updating court status
    Court updateCourtStatus(Long courtId, CourtStatus status);
}
