package com.pickleball.be.service.impl;

import com.pickleball.be.dto.CreateBookingDTO;
import com.pickleball.be.dto.booking.BookingHistoryResponse;
import com.pickleball.be.dto.booking.BookingRequest;
import com.pickleball.be.model.Booking;
import com.pickleball.be.model.Court;
import com.pickleball.be.model.SubCourt;
import com.pickleball.be.model.User;
import com.pickleball.be.repository.BookingRepository;
import com.pickleball.be.repository.CourtRepository;
import com.pickleball.be.repository.SubCourtRepository;
import com.pickleball.be.repository.UserRepository;
import com.pickleball.be.service.BookingService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class BookingServiceImpl implements BookingService {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourtRepository courtRepository;

    @Autowired
    private SubCourtRepository subCourtRepository;

    @Override
    @Transactional
    public Booking createBooking(CreateBookingDTO bookingDTO, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        
        Court court = courtRepository.findById(bookingDTO.getCourtId())
                .orElseThrow(() -> new EntityNotFoundException("Court not found"));

        // Lấy danh sách subCourt
        List<SubCourt> subCourts = new ArrayList<>();
        if (bookingDTO.getSubCourtIds() != null && !bookingDTO.getSubCourtIds().isEmpty()) {
            for (Long subCourtId : bookingDTO.getSubCourtIds()) {
                SubCourt subCourt = subCourtRepository.findById(subCourtId)
                        .orElseThrow(() -> new EntityNotFoundException("SubCourt not found: " + subCourtId));
                // Kiểm tra overlapping cho từng subCourt
                List<Booking> overlapping = bookingRepository.findOverlappingBookingsForSubCourt(
                    subCourtId, bookingDTO.getStartTime(), bookingDTO.getEndTime()
                );
                if (!overlapping.isEmpty()) {
                    throw new IllegalStateException("SubCourt " + subCourt.getName() + " is already booked in the selected time slot");
                }
                subCourts.add(subCourt);
            }
        } else {
            throw new IllegalArgumentException("subCourtIds is required");
        }

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setCourt(court);
        booking.setSubCourts(subCourts);
        booking.setStartTime(bookingDTO.getStartTime());
        booking.setEndTime(bookingDTO.getEndTime());
        booking.setStatus("PENDING");
        booking.setPaymentStatus("PENDING");
        booking.setPaymentMethod(bookingDTO.getPaymentMethod());
        booking.setNotes(bookingDTO.getNotes());
        booking.setTotalPrice(calculatePrice(court, bookingDTO.getStartTime(), bookingDTO.getEndTime()));

        return bookingRepository.save(booking);
    }

    @Override
    public Booking getBooking(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Booking not found"));
    }

    @Override
    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }

    @Override
    public List<Booking> getBookingsByUser(Long userId) {
        return bookingRepository.findByUserId(userId);
    }

    @Override
    public List<Booking> getBookingsByCourt(Long courtId) {
        return bookingRepository.findByCourtId(courtId);
    }

    @Override
    @Transactional
    public Booking updateBookingStatus(Long id, String status) {
        Booking booking = getBooking(id);
        booking.setStatus(status);
        return bookingRepository.save(booking);
    }

    @Override
    @Transactional
    public Booking updatePaymentStatus(Long id, String paymentStatus) {
        Booking booking = getBooking(id);
        booking.setPaymentStatus(paymentStatus);
        return bookingRepository.save(booking);
    }

    @Override
    @Transactional
    public void deleteBooking(Long id) {
        if (!bookingRepository.existsById(id)) {
            throw new EntityNotFoundException("Booking not found");
        }
        bookingRepository.deleteById(id);
    }

    @Override
    public List<Booking> getBookingsByCourtAndDateRange(Long courtId, LocalDateTime start, LocalDateTime end) {
        return bookingRepository.findByCourtAndDateRange(courtId, start, end);
    }

    @Override
    public List<Booking> createMultiBooking(BookingRequest req, Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new EntityNotFoundException("User not found"));
        Court court = courtRepository.findById(req.getCourtId()).orElseThrow(() -> new EntityNotFoundException("Court not found"));
        List<Booking> result = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
        for (BookingRequest.SubCourtBookingRequest b : req.getBookings()) {
            SubCourt subCourt = subCourtRepository.findById(b.getSubCourtId()).orElseThrow(() -> new EntityNotFoundException("SubCourt not found: " + b.getSubCourtId()));
            LocalDateTime start = LocalDateTime.parse(b.getStartTime(), formatter);
            LocalDateTime end = LocalDateTime.parse(b.getEndTime(), formatter);
            // Kiểm tra overlapping cho từng subCourt
            List<Booking> overlapping = bookingRepository.findOverlappingBookingsForSubCourt(subCourt.getId(), start, end);
            if (!overlapping.isEmpty()) throw new IllegalStateException("SubCourt " + subCourt.getName() + " is already booked in the selected time slot");
            Booking booking = new Booking();
            booking.setUser(user);
            booking.setCourt(court);
            booking.setSubCourts(List.of(subCourt));
            booking.setStartTime(start);
            booking.setEndTime(end);
            booking.setStatus("PENDING");
            booking.setPaymentStatus("PENDING");
            booking.setPaymentMethod(req.getPaymentMethod());
            booking.setNotes(req.getNotes());
            booking.setTotalPrice(calculatePrice(court, start, end));
            result.add(bookingRepository.save(booking));
        }
        return result;
    }

    private Double calculatePrice(Court court, LocalDateTime startTime, LocalDateTime endTime) {
        Double hourlyPrice = court.getHourlyPrice().doubleValue();
        Double totalPrice = 0.0;

        LocalDateTime currentIntervalStart = startTime;

        while (currentIntervalStart.isBefore(endTime)) {
            LocalDateTime currentIntervalEnd = currentIntervalStart.plusMinutes(30);

            // Ensure we don't go past the actual endTime of the booking
            if (currentIntervalEnd.isAfter(endTime)) {
                currentIntervalEnd = endTime;
            }

            long minutesInSlot = java.time.Duration.between(currentIntervalStart, currentIntervalEnd).toMinutes();
            Double currentSlotPrice = hourlyPrice * (minutesInSlot / 60.0);

            // Apply 10% increase for peak hours (from 17:00 onwards)
            if (currentIntervalStart.getHour() >= 17) {
                currentSlotPrice *= 1.10;
            }

            totalPrice += currentSlotPrice;
            currentIntervalStart = currentIntervalEnd;
        }

        return totalPrice;
    }

    @Override
    public List<BookingHistoryResponse> getOwnerCourtBookings(Long ownerId) {
        List<Court> ownerCourts = courtRepository.findByOwnerId(ownerId);
        if (ownerCourts.isEmpty()) {
            throw new EntityNotFoundException("No courts found for owner with ID: " + ownerId);
        }

        List<Booking> allBookings = new ArrayList<>();
        for (Court court : ownerCourts) {
            List<Booking> courtBookings = bookingRepository.findByCourtId(court.getId());
            allBookings.addAll(courtBookings);
        }

        return allBookings.stream()
            .map(b -> BookingHistoryResponse.builder()
                .id(b.getId())
                .courtId(b.getCourt().getId())
                .courtName(b.getCourt().getName())
                .subCourts(
                    b.getSubCourts() != null
                    ? b.getSubCourts().stream()
                        .map(sc -> BookingHistoryResponse.SubCourtInfo.builder()
                            .id(sc.getId())
                            .name(sc.getName())
                            .build())
                        .toList()
                    : List.of()
                )
                .startTime(b.getStartTime())
                .endTime(b.getEndTime())
                .status(b.getStatus())
                .totalPrice(b.getTotalPrice())
                .paymentStatus(b.getPaymentStatus())
                .paymentMethod(b.getPaymentMethod())
                .notes(b.getNotes())
                .createdAt(b.getCreatedAt())
                .build())
            .sorted((b1, b2) -> b2.getStartTime().compareTo(b1.getStartTime())) // Sort by start time descending
            .toList();
    }
} 