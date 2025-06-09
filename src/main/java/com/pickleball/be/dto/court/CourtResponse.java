package com.pickleball.be.dto.court;

import com.pickleball.be.model.CourtStatus;
import com.pickleball.be.model.CourtType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class CourtResponse {
    private Long id;
    private String name;
    private String address;
    private String description;
    private CourtType courtType;
    private List<String> imageUrls;
    private BigDecimal hourlyPrice;
    private CourtStatus status;
    private Long ownerId;
    private String ownerName;
    private LocalDateTime createdAt;
    private List<SubCourtResponse> subCourts;

    @Data
    @Builder
    public static class SubCourtResponse {
        private Long id;
        private String name;
        private CourtStatus status;
    }
}
