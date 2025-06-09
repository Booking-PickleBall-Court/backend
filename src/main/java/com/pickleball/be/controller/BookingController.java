package com.pickleball.be.controller;

import com.pickleball.be.dto.CreateBookingDTO;
import com.pickleball.be.model.Booking;
import com.pickleball.be.service.BookingService;
import com.pickleball.be.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.Map;
import com.pickleball.be.dto.booking.BookingHistoryResponse;
import com.pickleball.be.dto.booking.BookingDetailsResponse;

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
    public ResponseEntity<BookingDetailsResponse> getBooking(@PathVariable Long id) {
        Booking booking = bookingService.getBooking(id);
        BookingDetailsResponse response = BookingDetailsResponse.builder()
            .id(booking.getId())
            .court(BookingDetailsResponse.CourtInfo.builder()
                .id(booking.getCourt().getId())
                .name(booking.getCourt().getName())
                .address(booking.getCourt().getAddress())
                .description(booking.getCourt().getDescription())
                .courtType(booking.getCourt().getCourtType().name())
                .images(booking.getCourt().getImages().stream()
                    .map(img -> BookingDetailsResponse.ImageInfo.builder()
                        .id(img.getId())
                        .imageUrl(img.getImageUrl())
                        .build())
                    .toList())
                .build())
            .user(BookingDetailsResponse.UserInfo.builder()
                .id(booking.getUser().getId())
                .fullName(booking.getUser().getFullName())
                .email(booking.getUser().getEmail())
                .phoneNumber(booking.getUser().getPhoneNumber())
                .avatarUrl(booking.getUser().getAvatarUrl())
                .build())
            .subCourts(booking.getSubCourts().stream()
                .map(sc -> BookingDetailsResponse.SubCourtInfo.builder()
                    .id(sc.getId())
                    .name(sc.getName())
                    .build())
                .toList())
            .startTime(booking.getStartTime())
            .endTime(booking.getEndTime())
            .status(booking.getStatus())
            .totalPrice(booking.getTotalPrice())
            .paymentStatus(booking.getPaymentStatus())
            .paymentMethod(booking.getPaymentMethod())
            .notes(booking.getNotes())
            .createdAt(booking.getCreatedAt())
            .build();
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Booking>> getAllBookings() {
        return ResponseEntity.ok(bookingService.getAllBookings());
    }

    @GetMapping("/user")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<List<BookingHistoryResponse>> getUserBookings() {
        Long userId = userService.getCurrentUser().getId();
        List<Booking> bookings = bookingService.getBookingsByUser(userId);
        List<BookingHistoryResponse> responses = bookings.stream().map(b -> BookingHistoryResponse.builder()
                .id(b.getId())
                .courtId(b.getCourt().getId())
                .courtName(b.getCourt().getName())
                .subCourts(
                    b.getSubCourts() != null
                    ? b.getSubCourts().stream()
                        .map(sc -> BookingHistoryResponse.SubCourtInfo.builder()
                            .id(sc.getId())
                            .name(sc.getName())
                            .build())
                        .toList()
                    : List.of()
                )
                .startTime(b.getStartTime())
                .endTime(b.getEndTime())
                .status(b.getStatus())
                .totalPrice(b.getTotalPrice())
                .paymentStatus(b.getPaymentStatus())
                .paymentMethod(b.getPaymentMethod())
                .notes(b.getNotes())
                .createdAt(b.getCreatedAt())
                .build()
        ).toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/court/{courtId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENT')")
    public ResponseEntity<List<Booking>> getBookingsByCourt(@PathVariable Long courtId) {
        return ResponseEntity.ok(bookingService.getBookingsByCourt(courtId));
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

//    @GetMapping("/availability")
//    public ResponseEntity<Boolean> checkTimeSlotAvailability(
//            @RequestParam Long courtId,
//            @RequestParam LocalDateTime startTime,
//            @RequestParam LocalDateTime endTime) {
//        return ResponseEntity.ok(bookingService.isTimeSlotAvailable(courtId, startTime, endTime));
//    }

    @GetMapping("/slots")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<?> getBookedSlots(
            @RequestParam Long courtId,
            @RequestParam String date // dạng YYYY-MM-DD
    ) {
        LocalDate localDate = LocalDate.parse(date);
        LocalDateTime startOfDay = localDate.atStartOfDay();
        LocalDateTime endOfDay = localDate.atTime(LocalTime.MAX);

        List<Booking> bookings = bookingService.getBookingsByCourtAndDateRange(courtId, startOfDay, endOfDay);

        List<Object> bookedSlots = bookings.stream().map(b -> {
            List<Long> subCourtIds = b.getSubCourts() != null
                ? b.getSubCourts().stream().map(sc -> sc.getId()).collect(Collectors.toList())
                : new ArrayList<>();
            return Map.of(
                "startTime", b.getStartTime(),
                "endTime", b.getEndTime(),
                "subCourtIds", subCourtIds
            );
        }).collect(Collectors.toList());

        return ResponseEntity.ok(Map.of("bookedSlots", bookedSlots));
    }
} 