package com.pickleball.be.controller;

import com.pickleball.be.model.Booking;
import com.pickleball.be.service.BookingService;
import com.pickleball.be.service.PaymentService;
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

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final BookingService bookingService;
    private final PaymentService paymentService;
    private final UserService userService;

    @Value("${stripe.api.key}")
    private String stripeSecretKey;

    @Value("${stripe.webhook.secret}")
    private String webhookSecret;

    @PostMapping("/create-checkout-session")
    public ResponseEntity<Map<String, String>> createCheckoutSession(@RequestBody CreateBookingDTO bookingDTO) throws StripeException {
        Stripe.apiKey = stripeSecretKey;

        Long userId = userService.getCurrentUser().getId();
        Booking booking = bookingService.createBooking(bookingDTO, userId);

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl("http://localhost:3000/payment/success?session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl("http://localhost:3000/payment/cancel")
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                .setCurrency("usd")
                                .setUnitAmount((long) (booking.getTotalPrice() * 100))
                                .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                        .setName("Court Booking")
                                        .setDescription("Booking for court " + booking.getCourt().getName())
                                        .build())
                                .build())
                        .setQuantity(1L)
                        .build())
                .putMetadata("bookingId", booking.getId().toString())
                .build();

        Session session = Session.create(params);

        Map<String, String> responseData = new HashMap<>();
        responseData.put("id", session.getId());
        responseData.put("url", session.getUrl());

        return ResponseEntity.ok(responseData);
    }

    @PostMapping("/webhook")
    @Transactional
    public ResponseEntity<String> handleWebhook(
            @RequestHeader("Stripe-Signature") String sigHeader,
            @RequestBody String payload
    ) {
        Event event;
        try {
            // Xác thực payload
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            // Nếu signature không khớp
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
        }

        // Chỉ xử lý event checkout.session.completed
        if ("checkout.session.completed".equals(event.getType())) {
            // Ép kiểu an toàn
            EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
            Session session = deserializer.getObject()
                    .map(obj -> (Session)obj)
                    .orElseThrow(() -> new IllegalStateException("Unable to deserialize session object"));

            String bookingIdStr = session.getMetadata().get("bookingId");
            if (bookingIdStr != null) {
                Long bookingId = Long.valueOf(bookingIdStr);
                Booking booking = bookingService.getBooking(bookingId);

                if (booking == null) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Booking not found");
                }
                if (!"PENDING".equals(booking.getStatus())) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid booking status");
                }

                booking.setStatus("CONFIRMED");
                booking.setPaymentStatus("PAID");
                bookingService.updateBookingStatus(bookingId, "CONFIRMED");
                bookingService.updatePaymentStatus(bookingId, "PAID");
                return ResponseEntity.ok("Webhook processed successfully");
            }
        }

        // Những event khác (ví dụ payment_intent.succeeded) đều trả về 200 nhưng không xử lý
        return ResponseEntity.ok("");
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