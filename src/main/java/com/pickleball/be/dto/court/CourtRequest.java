package com.pickleball.be.dto.court;

import com.pickleball.be.model.CourtType;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CourtRequest {
    private String name;
    private String address;
    private String description;
    private BigDecimal hourlyPrice;
    private CourtType courtType;
} 