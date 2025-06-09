package com.pickleball.be.service;

import com.pickleball.be.model.Booking;
import com.pickleball.be.repository.BookingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class BookingCleanupService {
    private final BookingRepository bookingRepository;

    @Autowired
    public BookingCleanupService(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    @Scheduled(fixedRate = 60000) // Run every minute
    @Transactional
    public void cleanupPendingBookings() {
        LocalDateTime fiveMinutesAgo = LocalDateTime.now().minusMinutes(5);
        List<Booking> pendingBookings = bookingRepository.findByStatus("PENDING").stream()
            .filter(booking -> booking.getCreatedAt().isBefore(fiveMinutesAgo))
            .toList();

        for (Booking booking : pendingBookings) {
            bookingRepository.delete(booking);
        }
    }
} 