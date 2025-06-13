package com.pickleball.be.controller;

import com.pickleball.be.model.Booking;
import com.pickleball.be.service.BookingService;
import com.pickleball.be.service.PaymentService;
import com.pickleball.be.service.EmailService;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.pickleball.be.dto.CreateBookingDTO;
import com.pickleball.be.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import java.io.BufferedReader;
import java.util.Map;
import java.util.HashMap;
import com.stripe.net.Webhook;
import com.stripe.model.Event;
import org.springframework.transaction.annotation.Transactional;
import com.pickleball.be.dto.booking.BookingRequest;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Arrays;
import com.pickleball.be.dto.booking.BookingHistoryResponse;
import com.pickleball.be.dto.booking.BookingConfirmationResponse;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final BookingService bookingService;
    private final PaymentService paymentService;
    private final UserService userService;
    private final EmailService emailService;

    @Value("${stripe.api.key}")
    private String stripeSecretKey;

    @Value("${stripe.webhook.secret}")
    private String webhookSecret;

    @PostMapping("/create-checkout-session")
    public ResponseEntity<Map<String, String>> createCheckoutSession(@RequestBody BookingRequest req) throws StripeException {
        // Validate payment method
        if (req.getPaymentMethod() == null || req.getPaymentMethod().trim().isEmpty()) {
            throw new IllegalArgumentException("Payment method is required");
        }

        // If payment method is CASH, handle differently
        if ("CASH".equalsIgnoreCase(req.getPaymentMethod())) {
            // Create bookings without Stripe session
            List<Booking> bookings = bookingService.createMultiBooking(req, userService.getCurrentUser().getId());
            String bookingIds = bookings.stream()
                .map(b -> b.getId().toString())
                .collect(Collectors.joining(","));
            
            Map<String, String> responseData = new HashMap<>();
            responseData.put("paymentMethod", "CASH");
            responseData.put("bookingIds", bookingIds);
            responseData.put("totalAmount", String.valueOf(bookings.stream()
                .mapToDouble(Booking::getTotalPrice)
                .sum()));
            return ResponseEntity.ok(responseData);
        }

        // For online payment methods (CREDIT_CARD, etc.)
        Stripe.apiKey = stripeSecretKey;
        Long userId = userService.getCurrentUser().getId();
        
        // Create bookings
        List<Booking> bookings = bookingService.createMultiBooking(req, userId);
        double totalPrice = bookings.stream()
            .mapToDouble(Booking::getTotalPrice)
            .sum();
            
        String bookingIds = bookings.stream()
            .map(b -> b.getId().toString())
            .collect(Collectors.joining(","));

        // Create Stripe checkout session
        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl("https://picklenetweb.vercel.app/confirmBooking?bookingIds=" + bookingIds + "&status=success")
                .setCancelUrl("https://picklenetweb.vercel.app")
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                .setCurrency("vnd")
                                .setUnitAmount((long)totalPrice)
                                .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                        .setName("Court Booking")
                                        .setDescription("Booking for court " + bookings.get(0).getCourt().getName())
                                        .build())
                                .build())
                        .setQuantity(1L)
                        .build())
                .putMetadata("bookingIds", bookingIds)
                .build();

        Session session = Session.create(params);
        Map<String, String> responseData = new HashMap<>();
        responseData.put("id", session.getId());
        responseData.put("url", session.getUrl());
        responseData.put("paymentMethod", "ONLINE");
        responseData.put("bookingIds", bookingIds);
        responseData.put("totalAmount", String.valueOf(totalPrice));
        return ResponseEntity.ok(responseData);
    }

    @PostMapping("/webhook")
    @Transactional
    public ResponseEntity<List<BookingConfirmationResponse>> handleWebhook(
            @RequestHeader("Stripe-Signature") String sigHeader,
            @RequestBody String payload
    ) {
        Event event;
        try {
            // Xác thực payload
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            // Nếu signature không khớp
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }

        // Chỉ xử lý event checkout.session.completed
        if ("checkout.session.completed".equals(event.getType())) {
            // Ép kiểu an toàn
            EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
            Session session = deserializer.getObject()
                    .map(obj -> (Session)obj)
                    .orElseThrow(() -> new IllegalStateException("Unable to deserialize session object"));

            String bookingIdsStr = session.getMetadata().get("bookingIds");
            if (bookingIdsStr != null) {
                String[] bookingIdArray = bookingIdsStr.split(",");
                for (String bookingIdStr : bookingIdArray) {
                    Long bookingId = Long.valueOf(bookingIdStr);
                    Booking booking = bookingService.getBooking(bookingId);

                    if (booking == null) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
                    }
                    if (!"PENDING".equals(booking.getStatus())) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
                    }

                    booking.setStatus("CONFIRMED");
                    booking.setPaymentStatus("PAID");
                    bookingService.updateBookingStatus(bookingId, "CONFIRMED");
                    bookingService.updatePaymentStatus(bookingId, "PAID");

                    // Send email notification to court owner after successful payment
                    try {
                        emailService.sendBookingNotificationToOwner(booking);
                    } catch (Exception e) {
                        // Log the error but don't throw it to prevent affecting the payment process
                        System.err.println("Failed to send email notification: " + e.getMessage());
                    }
                }
                // Get updated booking information and convert to DTO
                List<BookingConfirmationResponse> updatedBookingResponses = Arrays.stream(bookingIdArray)
                    .map(id -> {
                        Booking b = bookingService.getBooking(Long.valueOf(id));
                        return BookingConfirmationResponse.builder()
                                .id(b.getId())
                                .court(BookingConfirmationResponse.CourtInfo.builder()
                                    .id(b.getCourt().getId())
                                    .name(b.getCourt().getName())
                                    .address(b.getCourt().getAddress())
                                    .description(b.getCourt().getDescription())
                                    .courtType(b.getCourt().getCourtType().name())
                                    .images(b.getCourt().getImages().stream()
                                        .map(img -> BookingConfirmationResponse.ImageInfo.builder()
                                            .id(img.getId())
                                            .imageUrl(img.getImageUrl())
                                            .build())
                                        .toList())
                                    .build())
                                .subCourts(b.getSubCourts().stream()
                                    .map(sc -> BookingConfirmationResponse.SubCourtInfo.builder()
                                        .id(sc.getId())
                                        .name(sc.getName())
                                        .build())
                                    .toList())
                                .startTime(b.getStartTime())
                                .endTime(b.getEndTime())
                                .status(b.getStatus())
                                .totalPrice(b.getTotalPrice())
                                .paymentStatus(b.getPaymentStatus())
                                .paymentMethod(b.getPaymentMethod())
                                .notes(b.getNotes())
                                .createdAt(b.getCreatedAt())
                                .build();
                    })
                    .toList();
                return ResponseEntity.ok(updatedBookingResponses);
            }
        }
        return ResponseEntity.ok().build();
    }

    private String getRequestBody(HttpServletRequest request) throws Exception {
        StringBuilder buffer = new StringBuilder();
        BufferedReader reader = request.getReader();
        String line;
        while ((line = reader.readLine()) != null) {
            buffer.append(line);
        }
        return buffer.toString();
    }
} 