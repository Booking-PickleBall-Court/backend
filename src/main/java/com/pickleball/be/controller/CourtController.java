package com.pickleball.be.controller;

import com.pickleball.be.dto.court.CourtRequest;
import com.pickleball.be.dto.court.CourtResponse;
import com.pickleball.be.dto.court.CourtRevenueResponse;
import com.pickleball.be.dto.court.OwnerRevenueResponse;
import com.pickleball.be.dto.court.MonthlyRevenueResponse;
import com.pickleball.be.dto.court.TopCustomerResponse;
import com.pickleball.be.model.Court;
import com.pickleball.be.model.CourtImage;
import com.pickleball.be.model.CourtStatus;
import com.pickleball.be.model.CourtType;
import com.pickleball.be.model.SubCourt;
import com.pickleball.be.service.CloudinaryImageService;
import com.pickleball.be.service.CourtService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/courts")
@RequiredArgsConstructor
public class CourtController {

    @Autowired
    private CloudinaryImageService cloudinaryImageService;

    @Autowired
    private final CourtService courtService;

    @PostMapping(consumes = "multipart/form-data")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<CourtResponse> createCourt(@ModelAttribute CourtRequest request) {
        Court court = courtService.createCourt(request);
        return ResponseEntity.ok(mapToCourtResponse(court));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('OWNER') and @courtServiceImpl.isCourtOwner(#id)")
    public ResponseEntity<CourtResponse> updateCourt(@PathVariable Long id, @ModelAttribute CourtRequest request) {
        Court court = courtService.updateCourt(id, request);
        return ResponseEntity.ok(mapToCourtResponse(court));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('OWNER') and @courtServiceImpl.isCourtOwner(#id)")
    public ResponseEntity<Void> deleteCourt(@PathVariable Long id) {
        courtService.deleteCourt(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<CourtResponse> getCourtById(@PathVariable Long id) {
        Court court = courtService.getCourtById(id);
        return ResponseEntity.ok(mapToCourtResponse(court));
    }

    @GetMapping
    public ResponseEntity<List<CourtResponse>> getAllCourts() {
        List<Court> courts = courtService.getAllCourts();
        List<CourtResponse> responses = courts.stream()
            .map(this::mapToCourtResponse)
            .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/owner/{ownerId}")
    public ResponseEntity<List<CourtResponse>> getCourtsByOwner(@PathVariable Long ownerId) {
        List<Court> courts = courtService.getCourtsByOwner(ownerId);
        List<CourtResponse> responses = courts.stream()
            .map(this::mapToCourtResponse)
            .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/available")
    public ResponseEntity<List<CourtResponse>> getAvailableCourts() {
        List<Court> courts = courtService.getAvailableCourts();
        List<CourtResponse> responses = courts.stream()
            .map(this::mapToCourtResponse)
            .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/filter")
    public ResponseEntity<Page<CourtResponse>> searchCourts(
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String address,
            @RequestParam(required = false) CourtType courtType,
            @RequestParam(required = false) CourtStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @PageableDefault(size = 9) Pageable pageable
    ) {
        Page<Court> courts = courtService.searchCourts(minPrice, maxPrice, address, courtType, status, date, pageable);
        Page<CourtResponse> responses = courts.map(this::mapToCourtResponse);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}/revenue")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<CourtRevenueResponse> getCourtRevenue(@PathVariable Long id) {
        return ResponseEntity.ok(courtService.getCourtRevenue(id));
    }

    @GetMapping("/revenue")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<CourtRevenueResponse>> getAllCourtsRevenue() {
        return ResponseEntity.ok(courtService.getAllCourtsRevenue());
    }

    @GetMapping("/owner/{ownerId}/revenue")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<OwnerRevenueResponse> getOwnerRevenue(@PathVariable Long ownerId) {
        return ResponseEntity.ok(courtService.getOwnerRevenue(ownerId));
    }

    @GetMapping("/owners/revenue")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<OwnerRevenueResponse>> getAllOwnersRevenue() {
        return ResponseEntity.ok(courtService.getAllOwnersRevenue());
    }

    @GetMapping("/{id}/monthly-revenue")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<List<MonthlyRevenueResponse>> getMonthlyRevenue(@PathVariable Long id) {
        return ResponseEntity.ok(courtService.getMonthlyRevenue(id));
    }

    @GetMapping("/owner/{ownerId}/monthly-revenue")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<List<MonthlyRevenueResponse>> getOwnerMonthlyRevenue(@PathVariable Long ownerId) {
        return ResponseEntity.ok(courtService.getOwnerMonthlyRevenue(ownerId));
    }

    @GetMapping("/monthly-revenue")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<MonthlyRevenueResponse>> getAllCourtsMonthlyRevenue() {
        return ResponseEntity.ok(courtService.getAllCourtsMonthlyRevenue());
    }

    @GetMapping("/owner/{ownerId}/top-customers")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<List<TopCustomerResponse>> getTopCustomers(@PathVariable Long ownerId) {
        return ResponseEntity.ok(courtService.getTopCustomers(ownerId));
    }

    private CourtResponse mapToCourtResponse(Court court) {
        List<String> imageUrls = court.getImages().stream()
                .map(CourtImage::getImageUrl)
                .collect(Collectors.toList());
        List<CourtResponse.SubCourtResponse> subCourtResponses = court.getSubCourts().stream()
                .map(subCourt -> CourtResponse.SubCourtResponse.builder()
                        .id(subCourt.getId())
                        .name(subCourt.getName())
                        .status(subCourt.getStatus())
                        .build())
                .collect(Collectors.toList());
        return CourtResponse.builder()
                .id(court.getId())
                .name(court.getName())
                .address(court.getAddress())
                .description(court.getDescription())
                .courtType(court.getCourtType())
                .imageUrls(imageUrls)
                .hourlyPrice(court.getHourlyPrice())
                .status(court.getStatus())
                .ownerId(court.getOwner().getId())
                .ownerName(court.getOwner().getFullName())
                .createdAt(court.getCreatedAt())
                .subCourts(subCourtResponses)
                .build();
    }
}
