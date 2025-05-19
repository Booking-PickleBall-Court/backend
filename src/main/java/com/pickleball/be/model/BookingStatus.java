package com.pickleball.be.model;

public enum BookingStatus {
    PENDING,    // Waiting for payment
    CONFIRMED,  // Payment received, booking confirmed
    CANCELLED,  // Booking cancelled
    COMPLETED   // Booking completed
} 