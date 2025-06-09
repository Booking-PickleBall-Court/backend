package com.pickleball.be.dto.booking;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class BookingConfirmationResponse {
    private Long id;
    private CourtInfo court;
    private List<SubCourtInfo> subCourts;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
    private Double totalPrice;
    private String paymentStatus;
    private String paymentMethod;
    private String notes;
    private LocalDateTime createdAt;

    @Data
    @Builder
    public static class CourtInfo {
        private Long id;
        private String name;
        private String address;
        private String description;
        private String courtType;
        private List<ImageInfo> images;
    }

    @Data
    @Builder
    public static class SubCourtInfo {
        private Long id;
        private String name;
    }

    @Data
    @Builder
    public static class ImageInfo {
        private Long id;
        private String imageUrl;
    }
} 