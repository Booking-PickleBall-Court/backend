package com.pickleball.be.service.impl;

import com.pickleball.be.model.Booking;
import com.pickleball.be.service.PaymentService;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StripePaymentServiceImpl implements PaymentService {

    @Override
    public String createPaymentIntent(Booking booking) throws StripeException {
        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount((long) (booking.getTotalPrice() * 100)) // Convert to cents
                .setCurrency("usd")
                .setAutomaticPaymentMethods(
                        PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                .setEnabled(true)
                                .build()
                )
                .putMetadata("bookingId", booking.getId().toString())
                .putMetadata("userId", booking.getUser().getId().toString())
                .build();

        PaymentIntent paymentIntent = PaymentIntent.create(params);
        return paymentIntent.getClientSecret();
    }

    @Override
    public void confirmPayment(String paymentIntentId) throws StripeException {
        PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);
        if (!paymentIntent.getStatus().equals("succeeded")) {
            throw new RuntimeException("Payment not successful for intent: " + paymentIntentId);
        }
    }

    @Override
    public void refundPayment(String paymentIntentId) throws StripeException {
        RefundCreateParams params = RefundCreateParams.builder()
            .setPaymentIntent(paymentIntentId)
            .build();
        Refund.create(params);
    }
} 