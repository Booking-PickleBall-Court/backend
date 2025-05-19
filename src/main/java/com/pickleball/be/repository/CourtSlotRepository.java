package com.pickleball.be.repository;

import com.pickleball.be.model.CourtSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CourtSlotRepository extends JpaRepository<CourtSlot, Long> {
    List<CourtSlot> findByCourtId(Long courtId);
    List<CourtSlot> findByStartTimeBetween(LocalDateTime start, LocalDateTime end);
    List<CourtSlot> findByCourtIdAndStartTimeBetween(Long courtId, LocalDateTime start, LocalDateTime end);
    List<CourtSlot> findByIsAvailableTrue();
    List<CourtSlot> findByCourtIdAndIsAvailableTrue(Long courtId);
} 