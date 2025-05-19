package com.pickleball.be.repository;

import com.pickleball.be.model.Booking;
import com.pickleball.be.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByUser(User user);
    
    @Query("SELECT b FROM Booking b WHERE b.user = ?1 AND b.status = ?2")
    List<Booking> findByUserAndStatus(User user, String status);

    List<Booking> findByUserId(Long userId);
    List<Booking> findByCourtSlotId(Long courtSlotId);
    List<Booking> findByStatus(String status);
    List<Booking> findByUserIdAndStatus(Long userId, String status);
    boolean existsByCourtSlotIdAndStatus(Long courtSlotId, String status);
} 