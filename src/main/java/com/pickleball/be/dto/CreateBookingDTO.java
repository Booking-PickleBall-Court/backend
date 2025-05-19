package com.pickleball.be.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateBookingDTO {
    private Long courtSlotId;
    private String paymentMethod;
    private String notes;
} 