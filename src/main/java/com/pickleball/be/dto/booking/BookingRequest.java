package com.pickleball.be.dto.booking;

import lombok.Data;
import java.util.List;

@Data
public class BookingRequest {
    private Long courtId;
    private List<SubCourtBookingRequest> bookings;
    private String notes;
    private String paymentMethod;

    @Data
    public static class SubCourtBookingRequest {
        private Long subCourtId;
        private String startTime; // ISO string
        private String endTime;
    }
} 