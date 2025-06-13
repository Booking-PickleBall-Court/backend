package com.pickleball.be.service.impl;

import com.pickleball.be.model.Booking;
import com.pickleball.be.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

@Service
public class EmailServiceImpl implements EmailService {

    @Autowired
    private JavaMailSender emailSender;

    @Override
    public void sendBookingNotificationToOwner(Booking booking) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(booking.getCourt().getOwner().getEmail());
        message.setSubject("New Booking Notification - " + booking.getCourt().getName());
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        String startTime = booking.getStartTime().format(formatter);
        String endTime = booking.getEndTime().format(formatter);
        
        String subCourts = booking.getSubCourts().stream()
            .map(sc -> sc.getName())
            .reduce((a, b) -> a + ", " + b)
            .orElse("");

        String content = String.format(
            "A new booking has been made for your court:\n\n" +
            "Court: %s\n" +
            "Sub Courts: %s\n" +
            "Date & Time: %s - %s\n" +
            "Customer: %s\n" +
            "Total Price: %.2f\n" + "VND" +
            "Payment Method: %s\n" +
            "Status: %s\n" +
            "Notes: %s",
            booking.getCourt().getName(),
            subCourts,
            startTime,
            endTime,
            booking.getUser().getFullName(),
            booking.getTotalPrice(),
            booking.getPaymentMethod(),
            booking.getStatus(),
            booking.getNotes() != null ? booking.getNotes() : "N/A"
        );
        
        message.setText(content);
        emailSender.send(message);
    }
} 