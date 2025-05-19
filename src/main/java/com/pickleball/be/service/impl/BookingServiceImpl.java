package com.pickleball.be.service.impl;

import com.pickleball.be.dto.CreateBookingDTO;
import com.pickleball.be.model.Booking;
import com.pickleball.be.model.CourtSlot;
import com.pickleball.be.model.User;
import com.pickleball.be.repository.BookingRepository;
import com.pickleball.be.repository.CourtSlotRepository;
import com.pickleball.be.repository.UserRepository;
import com.pickleball.be.service.BookingService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final CourtSlotRepository courtSlotRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public Booking createBooking(CreateBookingDTO bookingDTO, Long userId) {
        // Check if court slot exists and is available
        CourtSlot courtSlot = courtSlotRepository.findById(bookingDTO.getCourtSlotId())
                .orElseThrow(() -> new EntityNotFoundException("Court slot not found"));

        if (!courtSlot.isAvailable()) {
            throw new IllegalStateException("Court slot is not available");
        }

        // Check if there's any pending booking for this slot
        if (bookingRepository.existsByCourtSlotIdAndStatus(bookingDTO.getCourtSlotId(), "PENDING")) {
            throw new IllegalStateException("This slot is being processed by another user");
        }

        // Check if user exists
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        // Mark slot as unavailable
        courtSlot.setAvailable(false);
        courtSlot.setStatus("PENDING");
        courtSlotRepository.save(courtSlot);

        // Create new booking
        Booking booking = new Booking();
        booking.setCourtSlot(courtSlot);
        booking.setUser(user);
        booking.setBookingTime(LocalDateTime.now());
        booking.setStatus("PENDING");
        booking.setTotalPrice(courtSlot.getPrice());
        booking.setPaymentStatus("PENDING");
        booking.setPaymentMethod(bookingDTO.getPaymentMethod());
        booking.setNotes(bookingDTO.getNotes());

        return bookingRepository.save(booking);
    }

    @Override
    public Booking getBooking(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Booking not found"));
    }

    @Override
    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }

    @Override
    public List<Booking> getBookingsByUser(Long userId) {
        return bookingRepository.findByUserId(userId);
    }

    @Override
    public List<Booking> getBookingsByCourtSlot(Long courtSlotId) {
        return bookingRepository.findByCourtSlotId(courtSlotId);
    }

    @Override
    @Transactional
    public Booking updateBookingStatus(Long id, String status) {
        Booking booking = getBooking(id);
        String oldStatus = booking.getStatus();
        booking.setStatus(status);

        // Update slot availability based on booking status
        CourtSlot courtSlot = booking.getCourtSlot();
        if (status.equals("CANCELLED") || status.equals("REJECTED")) {
            courtSlot.setAvailable(true);
            courtSlot.setStatus("AVAILABLE");
        } else if (status.equals("CONFIRMED")) {
            courtSlot.setAvailable(false);
            courtSlot.setStatus("BOOKED");
        }
        courtSlotRepository.save(courtSlot);

        return bookingRepository.save(booking);
    }

    @Override
    @Transactional
    public Booking updatePaymentStatus(Long id, String paymentStatus) {
        Booking booking = getBooking(id);
        String oldPaymentStatus = booking.getPaymentStatus();
        booking.setPaymentStatus(paymentStatus);

        // If payment fails, make the slot available again
        if (paymentStatus.equals("FAILED")) {
            CourtSlot courtSlot = booking.getCourtSlot();
            courtSlot.setAvailable(true);
            courtSlot.setStatus("AVAILABLE");
            courtSlotRepository.save(courtSlot);
            
            // Update booking status to cancelled
            booking.setStatus("CANCELLED");
        }

        return bookingRepository.save(booking);
    }

    @Override
    @Transactional
    public void deleteBooking(Long id) {
        Booking booking = getBooking(id);
        
        // Make court slot available again
        CourtSlot courtSlot = booking.getCourtSlot();
        courtSlot.setAvailable(true);
        courtSlot.setStatus("AVAILABLE");
        courtSlotRepository.save(courtSlot);
        
        bookingRepository.delete(booking);
    }

    @Override
    public boolean isCourtSlotAvailable(Long courtSlotId) {
        CourtSlot courtSlot = courtSlotRepository.findById(courtSlotId)
                .orElseThrow(() -> new EntityNotFoundException("Court slot not found"));
        return courtSlot.isAvailable() && 
               !bookingRepository.existsByCourtSlotIdAndStatus(courtSlotId, "PENDING");
    }
} 