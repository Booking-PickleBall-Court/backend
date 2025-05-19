package com.pickleball.be.service;

import com.pickleball.be.model.Booking;
import com.stripe.exception.StripeException;

public interface PaymentService {
    String createPaymentIntent(Booking booking) throws StripeException;
    void confirmPayment(String paymentIntentId) throws StripeException;
    void refundPayment(String paymentIntentId) throws StripeException;
} 