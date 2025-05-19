package com.pickleball.be.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CourtSlotDTO {
    private Long id;
    private Long courtId;
    private LocalDate date;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private boolean isAvailable;
    private Double price;
    private String status;
} 