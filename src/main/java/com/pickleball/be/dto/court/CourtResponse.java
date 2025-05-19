package com.pickleball.be.dto.court;

import com.pickleball.be.model.CourtStatus;
import com.pickleball.be.model.CourtType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class CourtResponse {
    private Long id;
    private String name;
    private String address;
    private String description;
    private CourtType courtType;
    private String imageUrl;
    private BigDecimal hourlyPrice;
    private CourtStatus status;
    private Long ownerId;
    private String ownerName;
    private LocalDateTime createdAt;
}
