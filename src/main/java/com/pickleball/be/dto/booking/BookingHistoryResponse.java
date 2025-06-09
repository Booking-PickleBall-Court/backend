package com.pickleball.be.dto.booking;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class BookingHistoryResponse {
    private Long id;
    private Long courtId;
    private String courtName;
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
    public static class SubCourtInfo {
        private Long id;
        private String name;
    }
} 