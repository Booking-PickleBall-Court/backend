package com.pickleball.be.service;

import com.pickleball.be.dto.court.CourtRequest;
import com.pickleball.be.dto.court.TimeSlotResponse;
import com.pickleball.be.model.Court;
import com.pickleball.be.model.CourtStatus;
import com.pickleball.be.model.CourtType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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
}
