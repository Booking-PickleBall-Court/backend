package com.pickleball.be.service.impl;

import com.pickleball.be.dto.CreateBookingDTO;
import com.pickleball.be.model.Booking;
import com.pickleball.be.model.Court;
import com.pickleball.be.model.User;
import com.pickleball.be.repository.BookingRepository;
import com.pickleball.be.repository.CourtRepository;
import com.pickleball.be.repository.UserRepository;
import com.pickleball.be.service.BookingService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class BookingServiceImpl implements BookingService {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourtRepository courtRepository;

    @Override
    @Transactional
    public Booking createBooking(CreateBookingDTO bookingDTO, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        
        Court court = courtRepository.findById(bookingDTO.getCourtId())
                .orElseThrow(() -> new EntityNotFoundException("Court not found"));

        // Check for overlapping bookings
        List<Booking> overlappingBookings = bookingRepository.findOverlappingBookings(
            court.getId(), 
            bookingDTO.getStartTime(), 
            bookingDTO.getEndTime()
        );

        if (!overlappingBookings.isEmpty()) {
            throw new IllegalStateException("The selected time slot is already booked");
        }

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setCourt(court);
        booking.setStartTime(bookingDTO.getStartTime());
        booking.setEndTime(bookingDTO.getEndTime());
        booking.setStatus("PENDING");
        booking.setPaymentStatus("PENDING");
        booking.setPaymentMethod(bookingDTO.getPaymentMethod());
        booking.setNotes(bookingDTO.getNotes());
        booking.setTotalPrice(calculatePrice(court, bookingDTO.getStartTime(), bookingDTO.getEndTime()));

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
    public List<Booking> getBookingsByCourt(Long courtId) {
        return bookingRepository.findByCourtId(courtId);
    }

    @Override
    @Transactional
    public Booking updateBookingStatus(Long id, String status) {
        Booking booking = getBooking(id);
        booking.setStatus(status);
        return bookingRepository.save(booking);
    }

    @Override
    @Transactional
    public Booking updatePaymentStatus(Long id, String paymentStatus) {
        Booking booking = getBooking(id);
        booking.setPaymentStatus(paymentStatus);
        return bookingRepository.save(booking);
    }

    @Override
    @Transactional
    public void deleteBooking(Long id) {
        if (!bookingRepository.existsById(id)) {
            throw new EntityNotFoundException("Booking not found");
        }
        bookingRepository.deleteById(id);
    }

    @Override
    public boolean isTimeSlotAvailable(Long courtId, LocalDateTime startTime, LocalDateTime endTime) {
        List<Booking> overlappingBookings = bookingRepository.findOverlappingBookings(courtId, startTime, endTime);
        return overlappingBookings.isEmpty();
    }

    private Double calculatePrice(Court court, LocalDateTime startTime, LocalDateTime endTime) {
        // Implement your pricing logic here
        // This is a simple example - you might want to add more complex pricing rules
        long hours = java.time.Duration.between(startTime, endTime).toHours();
        return court.getHourlyPrice().doubleValue() * hours;
    }
} 