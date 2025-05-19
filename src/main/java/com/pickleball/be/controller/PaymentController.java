package com.pickleball.be.controller;

import com.pickleball.be.model.Booking;
import com.pickleball.be.service.BookingService;
import com.pickleball.be.service.PaymentService;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
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
import com.pickleball.be.service.CourtSlotService;
import com.pickleball.be.repository.CourtSlotRepository;
import com.stripe.net.Webhook;
import com.stripe.model.Event;
import com.pickleball.be.model.CourtSlot;
import org.springframework.transaction.annotation.Transactional;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final BookingService bookingService;
    private final UserService userService;
    private final CourtSlotService courtSlotService;
    private final CourtSlotRepository courtSlotRepository;

    @Value("${stripe.webhook.secret:}")
    private String endpointSecret;

    @PostMapping("/create-payment-intent/{bookingId}")
    public ResponseEntity<Map<String, String>> createPaymentIntent(@PathVariable Long bookingId) throws StripeException {
        Booking booking = bookingService.getBooking(bookingId);
        String clientSecret = paymentService.createPaymentIntent(booking);
        return ResponseEntity.ok(Map.of("clientSecret", clientSecret));
    }

    @PostMapping("/confirm/{paymentIntentId}")
    public ResponseEntity<Void> confirmPayment(@PathVariable String paymentIntentId) throws StripeException {
        paymentService.confirmPayment(paymentIntentId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/refund/{paymentIntentId}")
    public ResponseEntity<Void> refundPayment(@PathVariable String paymentIntentId) throws StripeException {
        paymentService.refundPayment(paymentIntentId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/create-checkout-session")
    public ResponseEntity<Map<String, String>> createCheckoutSession(@RequestBody Map<String, Object> data) throws Exception {
        try {
            Stripe.apiKey = "sk_test_51RQBWB2Lb4R1XJbp0WsDA3DXynHX5uA9x9VJ6hGfanX3wCa8aeLMVSavVhgZqiahfIQEcTMUatbDlTjnBd7o6Gan00wgOIfagH";

            // 1. Tạo booking trước
            Long courtSlotId = ((Number) data.get("courtSlotId")).longValue();
            String paymentMethod = (String) data.getOrDefault("paymentMethod", "CARD");
            String notes = (String) data.getOrDefault("notes", "");
            Long userId = userService.getCurrentUser().getId();
            CreateBookingDTO bookingDTO = new CreateBookingDTO(courtSlotId, paymentMethod, notes);
            Booking booking = bookingService.createBooking(bookingDTO, userId);

            long amount = ((Number) data.getOrDefault("amount", 10000)).longValue();
            String currency = (String) data.getOrDefault("currency", "usd");
            String productName = (String) data.getOrDefault("productName", "Court Slot Booking");

            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl("http://localhost:3000/bookings?payment=success")
                    .setCancelUrl("http://localhost:3000/bookings?payment=cancel")
                    .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setQuantity(1L)
                                    .setPriceData(
                                            SessionCreateParams.LineItem.PriceData.builder()
                                                    .setCurrency(currency)
                                                    .setUnitAmount(amount)
                                                    .setProductData(
                                                            SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                    .setName(productName)
                                                                    .build()
                                                    )
                                                    .build()
                                    )
                                    .build()
                    )
                    .putMetadata("bookingId", booking.getId().toString())
                    .build();

            Session session = Session.create(params);

            return ResponseEntity.ok(Map.of("url", session.getUrl()));
        } catch (Exception e) {
            e.printStackTrace(); // Log lỗi chi tiết ra console
            throw e;
        }


    }

    // Stripe webhook endpoint
    @PostMapping("/webhook")
    @Transactional
    public ResponseEntity<String> handleStripeWebhook(HttpServletRequest request) {
        String payload;
        String sigHeader = request.getHeader("Stripe-Signature");

        try {
            // Log headers for debugging
            System.out.println("[Stripe Webhook] Headers:");
            java.util.Enumeration<String> headerNames = request.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                System.out.println(headerName + ": " + request.getHeader(headerName));
            }

            // Read request body
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = request.getReader()) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            }
            payload = sb.toString();

            // Log payload and signature for debugging
            System.out.println("[Stripe Webhook] Payload: " + payload);
            System.out.println("[Stripe Webhook] Signature: " + sigHeader);
            System.out.println("[Stripe Webhook] Endpoint Secret: " + endpointSecret);

            String host = request.getHeader("host");
            if (host != null && host.contains("ngrok")) {
                // Parse manually for ngrok requests
                ObjectMapper mapper = new ObjectMapper();
                mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(payload);
                String eventType = root.get("type").asText();

                if ("checkout.session.completed".equals(eventType)) {
                    com.fasterxml.jackson.databind.JsonNode sessionNode = root.get("data").get("object");
                    com.fasterxml.jackson.databind.JsonNode metadataNode = sessionNode.get("metadata");
                    if (metadataNode == null || metadataNode.get("bookingId") == null) {
                        System.err.println("[Stripe Webhook] bookingId not found in metadata");
                        return ResponseEntity.status(400).body("Missing bookingId in metadata");
                    }
                    String bookingIdStr = metadataNode.get("bookingId").asText();
                    try {
                        Long bookingId = Long.valueOf(bookingIdStr);
                        Booking booking = bookingService.getBooking(bookingId);

                        if (booking == null) {
                            System.err.println("[Stripe Webhook] Booking not found for id=" + bookingId);
                            return ResponseEntity.status(404).body("Booking not found");
                        }

                        if (!"PENDING".equals(booking.getStatus())) {
                            System.err.println("[Stripe Webhook] Invalid booking status: " + booking.getStatus());
                            return ResponseEntity.status(400).body("Invalid booking status");
                        }

                        booking.setStatus("CONFIRMED");
                        booking.setPaymentStatus("PAID");
                        bookingService.updateBookingStatus(bookingId, "CONFIRMED");
                        bookingService.updatePaymentStatus(bookingId, "PAID");

                        CourtSlot slot = booking.getCourtSlot();
                        slot.setAvailable(false);
                        slot.setStatus("BOOKED");
                        courtSlotRepository.save(slot);

                        System.out.println("[Stripe Webhook] Successfully updated booking and slot for bookingId=" + bookingId);
                        return ResponseEntity.ok("Webhook processed successfully");
                    } catch (Exception ex) {
                        System.err.println("[Stripe Webhook] Error processing webhook: " + ex.getMessage());
                        ex.printStackTrace();
                        throw ex;
                    }
                }
                return ResponseEntity.ok("Event type not handled");
            } else {
                // Production: verify signature and use Stripe SDK
                Event event;
                try {
                    event = Webhook.constructEvent(payload, sigHeader, endpointSecret);
                } catch (Exception e) {
                    System.err.println("[Stripe Webhook] Invalid signature: " + e.getMessage());
                    System.err.println("[Stripe Webhook] Error details: " + e.toString());
                    return ResponseEntity.status(400).body("Invalid signature");
                }

                if ("checkout.session.completed".equals(event.getType())) {
                    Session session = (Session) event.getDataObjectDeserializer().getObject().orElse(null);
                    if (session == null || session.getMetadata() == null) {
                        System.err.println("[Stripe Webhook] Session or metadata is null");
                        return ResponseEntity.status(400).body("Invalid session data");
                    }

                    String bookingIdStr = session.getMetadata().get("bookingId");
                    if (bookingIdStr == null) {
                        System.err.println("[Stripe Webhook] bookingId not found in metadata");
                        return ResponseEntity.status(400).body("Missing bookingId in metadata");
                    }

                    try {
                        Long bookingId = Long.valueOf(bookingIdStr);
                        Booking booking = bookingService.getBooking(bookingId);

                        if (booking == null) {
                            System.err.println("[Stripe Webhook] Booking not found for id=" + bookingId);
                            return ResponseEntity.status(404).body("Booking not found");
                        }

                        if (!"PENDING".equals(booking.getStatus())) {
                            System.err.println("[Stripe Webhook] Invalid booking status: " + booking.getStatus());
                            return ResponseEntity.status(400).body("Invalid booking status");
                        }

                        booking.setStatus("CONFIRMED");
                        booking.setPaymentStatus("PAID");
                        bookingService.updateBookingStatus(bookingId, "CONFIRMED");
                        bookingService.updatePaymentStatus(bookingId, "PAID");

                        CourtSlot slot = booking.getCourtSlot();
                        slot.setAvailable(false);
                        slot.setStatus("BOOKED");
                        courtSlotRepository.save(slot);

                        System.out.println("[Stripe Webhook] Successfully updated booking and slot for bookingId=" + bookingId);
                        return ResponseEntity.ok("Webhook processed successfully");
                    } catch (Exception ex) {
                        System.err.println("[Stripe Webhook] Error processing webhook: " + ex.getMessage());
                        ex.printStackTrace();
                        throw ex;
                    }
                }
                return ResponseEntity.ok("Event type not handled");
            }
        } catch (Exception e) {
            System.err.println("[Stripe Webhook] General error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body("Internal server error");
        }
    }
} 