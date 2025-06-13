package com.pickleball.be.service;

import com.pickleball.be.model.Booking;

public interface EmailService {
    void sendBookingNotificationToOwner(Booking booking);
} 