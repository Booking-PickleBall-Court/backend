package com.pickleball.be.repository;

import com.pickleball.be.model.Booking;
import com.pickleball.be.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByUser(User user);
    
    @Query("SELECT b FROM Booking b WHERE b.user = ?1 AND b.status = ?2")
    List<Booking> findByUserAndStatus(User user, String status);

    List<Booking> findByUserId(Long userId);
    List<Booking> findByCourtId(Long courtId);
    List<Booking> findByStatus(String status);
    List<Booking> findByUserIdAndStatus(Long userId, String status);
    
    @Query("SELECT b FROM Booking b WHERE b.court.id = ?1 AND b.startTime <= ?2 AND b.endTime >= ?3 AND b.status = 'CONFIRMED'")
    List<Booking> findOverlappingBookings(Long courtId, LocalDateTime startTime, LocalDateTime endTime);
    
    @Query("SELECT b FROM Booking b WHERE b.court.id = ?1 AND b.startTime >= ?2 AND b.startTime < ?3")
    List<Booking> findByCourtAndDateRange(Long courtId, LocalDateTime startDate, LocalDateTime endDate);
} 