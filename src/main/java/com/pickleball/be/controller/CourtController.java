package com.pickleball.be.controller;

import com.pickleball.be.dto.court.CourtRequest;
import com.pickleball.be.model.Court;
import com.pickleball.be.model.CourtStatus;
import com.pickleball.be.model.CourtType;
import com.pickleball.be.service.CourtService;
import lombok.RequiredArgsConstructor;
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

@RestController
@RequestMapping("/api/courts")
@RequiredArgsConstructor
public class CourtController {

    private final CourtService courtService;

    @PostMapping
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<Court> createCourt(@RequestBody CourtRequest request) {
        return ResponseEntity.ok(courtService.createCourt(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('OWNER') and @courtServiceImpl.isCourtOwner(#id)")
    public ResponseEntity<Court> updateCourt(@PathVariable Long id, @RequestBody CourtRequest request) {
        return ResponseEntity.ok(courtService.updateCourt(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<Void> deleteCourt(@PathVariable Long id) {
        courtService.deleteCourt(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Court> getCourtById(@PathVariable Long id) {
        return ResponseEntity.ok(courtService.getCourtById(id));
    }

    @GetMapping
    public ResponseEntity<List<Court>> getAllCourts() {
        return ResponseEntity.ok(courtService.getAllCourts());
    }

    @GetMapping("/owner/{ownerId}")
    public ResponseEntity<List<Court>> getCourtsByOwner(@PathVariable Long ownerId) {
        return ResponseEntity.ok(courtService.getCourtsByOwner(ownerId));
    }

    @GetMapping("/available")
    public ResponseEntity<List<Court>> getAvailableCourts() {
        return ResponseEntity.ok(courtService.getAvailableCourts());
    }

    @GetMapping("/filter")
    public ResponseEntity<Page<Court>> searchCourts(
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String address,
            @RequestParam(required = false) CourtType courtType,
            @RequestParam(required = false) CourtStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @PageableDefault(size = 9) Pageable pageable
    ) {
        return ResponseEntity.ok(courtService.searchCourts(minPrice, maxPrice, address, courtType, status, date, pageable));
    }
}
