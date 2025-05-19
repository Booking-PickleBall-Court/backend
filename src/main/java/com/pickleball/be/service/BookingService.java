package com.pickleball.be.service;

import com.pickleball.be.dto.CreateBookingDTO;
import com.pickleball.be.model.Booking;

import java.util.List;

public interface BookingService {
    Booking createBooking(CreateBookingDTO bookingDTO, Long userId);
    Booking getBooking(Long id);
    List<Booking> getAllBookings();
    List<Booking> getBookingsByUser(Long userId);
    List<Booking> getBookingsByCourtSlot(Long courtSlotId);
    Booking updateBookingStatus(Long id, String status);
    Booking updatePaymentStatus(Long id, String paymentStatus);
    void deleteBooking(Long id);
    boolean isCourtSlotAvailable(Long courtSlotId);
} 