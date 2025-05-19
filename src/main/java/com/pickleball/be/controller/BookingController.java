package com.pickleball.be.controller;

import com.pickleball.be.dto.CreateBookingDTO;
import com.pickleball.be.model.Booking;
import com.pickleball.be.service.BookingService;
import com.pickleball.be.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final UserService userService;

    @PostMapping
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<Booking> createBooking(@RequestBody CreateBookingDTO bookingDTO) {
        Long userId = userService.getCurrentUser().getId();
        return ResponseEntity.ok(bookingService.createBooking(bookingDTO, userId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENT')")
    public ResponseEntity<Booking> getBooking(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.getBooking(id));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Booking>> getAllBookings() {
        return ResponseEntity.ok(bookingService.getAllBookings());
    }

    @GetMapping("/user")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<List<Booking>> getUserBookings() {
        Long userId = userService.getCurrentUser().getId();
        return ResponseEntity.ok(bookingService.getBookingsByUser(userId));
    }

    @GetMapping("/court-slot/{courtSlotId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENT')")
    public ResponseEntity<List<Booking>> getBookingsByCourtSlot(@PathVariable Long courtSlotId) {
        return ResponseEntity.ok(bookingService.getBookingsByCourtSlot(courtSlotId));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Booking> updateBookingStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        return ResponseEntity.ok(bookingService.updateBookingStatus(id, status));
    }

    @PutMapping("/{id}/payment-status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Booking> updatePaymentStatus(
            @PathVariable Long id,
            @RequestParam String paymentStatus) {
        return ResponseEntity.ok(bookingService.updatePaymentStatus(id, paymentStatus));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENT')")
    public ResponseEntity<Void> deleteBooking(@PathVariable Long id) {
        bookingService.deleteBooking(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/court-slot/{courtSlotId}/availability")
    public ResponseEntity<Boolean> checkCourtSlotAvailability(@PathVariable Long courtSlotId) {
        return ResponseEntity.ok(bookingService.isCourtSlotAvailable(courtSlotId));
    }
} 