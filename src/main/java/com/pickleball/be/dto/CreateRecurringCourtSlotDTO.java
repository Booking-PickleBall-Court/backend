package com.pickleball.be.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateRecurringCourtSlotDTO {
    private Long courtId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDate startDate;
    private LocalDate endDate;
    private Double price;
    private String status;
} 