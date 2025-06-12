package com.pickleball.be.service;

import com.pickleball.be.dto.CreateBookingDTO;
import com.pickleball.be.model.Booking;
import com.pickleball.be.dto.booking.BookingRequest;
import com.pickleball.be.dto.booking.BookingHistoryResponse;

import java.time.LocalDateTime;
import java.util.List;

public interface BookingService {
    Booking createBooking(CreateBookingDTO bookingDTO, Long userId);
    Booking getBooking(Long id);
    List<Booking> getAllBookings();
    List<Booking> getBookingsByUser(Long userId);
    List<Booking> getBookingsByCourt(Long courtId);
    Booking updateBookingStatus(Long id, String status);
    Booking updatePaymentStatus(Long id, String paymentStatus);
    void deleteBooking(Long id);
    List<Booking> getBookingsByCourtAndDateRange(Long courtId, LocalDateTime start, LocalDateTime end);
    List<Booking> createMultiBooking(BookingRequest req, Long userId);
    List<BookingHistoryResponse> getOwnerCourtBookings(Long ownerId);
    List<Booking> createMultiBookingForOwner(BookingRequest req, Long ownerId);
} 