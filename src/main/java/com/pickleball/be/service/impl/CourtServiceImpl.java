package com.pickleball.be.service.impl;

import com.pickleball.be.dto.court.CourtRequest;
import com.pickleball.be.dto.court.SubCourtRequest;
import com.pickleball.be.dto.court.CourtRevenueResponse;
import com.pickleball.be.dto.court.OwnerRevenueResponse;
import com.pickleball.be.dto.court.MonthlyRevenueResponse;
import com.pickleball.be.dto.court.TopCustomerResponse;
import com.pickleball.be.model.*;
import com.pickleball.be.repository.CourtRepository;
import com.pickleball.be.repository.CourtImageRepository;
import com.pickleball.be.repository.UserRepository;
import com.pickleball.be.repository.BookingRepository;
import com.pickleball.be.service.CourtService;
import com.pickleball.be.service.CloudinaryService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service("courtServiceImpl")
@RequiredArgsConstructor
public class CourtServiceImpl implements CourtService {

    private final CourtRepository courtRepository;
    private final UserRepository userRepository;
    private final CourtImageRepository courtImageRepository;
    private final CloudinaryService cloudinaryService;
    private final BookingRepository bookingRepository;

    @Override
    @Transactional
    public Court createCourt(CourtRequest request) {
        User currentOwner = getCurrentUser();
        Court court = new Court();
        mapToEntity(request, court);
        court.setOwner(currentOwner);
        court.setStatus(CourtStatus.AVAILABLE);

        court.getSubCourts().clear();
        if (request.getSubCourts() != null) {
            for (SubCourtRequest subCourtReq : request.getSubCourts()) {
                SubCourt subCourt = new SubCourt();
                subCourt.setName(subCourtReq.getName());
                subCourt.setCourt(court);
                court.getSubCourts().add(subCourt);
            }
        }

        if (request.getImages() != null && !request.getImages().isEmpty()) {
            try {
                List<Map<String, String>> uploadResults = cloudinaryService.uploadImages(request.getImages());
                for (Map<String, String> result : uploadResults) {
                    CourtImage courtImage = new CourtImage();
                    courtImage.setCourt(court);
                    courtImage.setImageUrl(result.get("url"));
                    courtImage.setCloudinaryPublicId(result.get("public_id"));
                    court.getImages().add(courtImage);
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to upload images", e);
            }
        }

        return courtRepository.save(court);
    }

    @Override
    @Transactional
    public Court updateCourt(Long id, CourtRequest request) {
        Court court = getCourtById(id);
        mapToEntity(request, court);

        court.getSubCourts().clear();
        if (request.getSubCourts() != null) {
            for (SubCourtRequest subCourtReq : request.getSubCourts()) {
                SubCourt subCourt = new SubCourt();
                subCourt.setName(subCourtReq.getName());
                subCourt.setCourt(court);
                court.getSubCourts().add(subCourt);
            }
        }

        if (request.getImages() != null && !request.getImages().isEmpty()) {
            // Delete existing images from Cloudinary
            court.getImages().forEach(image -> {
                try {
                    cloudinaryService.deleteImage(image.getCloudinaryPublicId());
                } catch (IOException e) {
                    throw new RuntimeException("Failed to delete old image", e);
                }
            });

            // Clear existing images
            court.getImages().clear();

            // Upload new images
            try {
                List<Map<String, String>> uploadResults = cloudinaryService.uploadImages(request.getImages());
                for (Map<String, String> result : uploadResults) {
                    CourtImage courtImage = new CourtImage();
                    courtImage.setCourt(court);
                    courtImage.setImageUrl(result.get("url"));
                    courtImage.setCloudinaryPublicId(result.get("public_id"));
                    court.getImages().add(courtImage);
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to upload images", e);
            }
        }

        return courtRepository.save(court);
    }

    @Override
    @Transactional
    public void deleteCourt(Long id) {
        Court court = getCourtById(id);
        
        // Delete images from Cloudinary
        court.getImages().forEach(image -> {
            try {
                cloudinaryService.deleteImage(image.getCloudinaryPublicId());
            } catch (IOException e) {
                throw new RuntimeException("Failed to delete image", e);
            }
        });
        
        courtRepository.delete(court);
    }

    @Override
    public Court getCourtById(Long id) {
        return courtRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Court not found"));
    }

    @Override
    public List<Court> getAllCourts() {
        return courtRepository.findAll();
    }

    @Override
    public List<Court> getCourtsByOwner(Long ownerId) {
        return courtRepository.findByOwnerId(ownerId);
    }

    @Override
    public List<Court> getAvailableCourts() {
        return courtRepository.findAll((root, query, cb) ->
                cb.equal(root.get("status"), CourtStatus.AVAILABLE));
    }

    @Override
    public List<Court> getCourtsByMaxPrice(BigDecimal maxPrice) {
        return courtRepository.findAll((root, query, cb) ->
                cb.lessThanOrEqualTo(root.get("hourlyPrice"), maxPrice));
    }

    @Override
    public Page<Court> searchCourts(BigDecimal minPrice, BigDecimal maxPrice, String address,
                                    CourtType courtType, CourtStatus status, LocalDate date, Pageable pageable) {
        Specification<Court> spec = Specification.where(null);

        if (minPrice != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("hourlyPrice"), minPrice));
        }

        if (maxPrice != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("hourlyPrice"), maxPrice));
        }

        if (address != null && !address.isBlank()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("address")), "%" + address.toLowerCase() + "%"));
        }

        if (courtType != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("courtType"), courtType));
        }

        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }

        if (date != null) {
            spec = spec.and((root, query, cb) -> {
                var subquery = query.subquery(Long.class);
                var bookingRoot = subquery.from(Booking.class);
                subquery.select(bookingRoot.get("court").get("id"))
                        .where(cb.and(
                            cb.equal(bookingRoot.get("court"), root),
                            cb.equal(bookingRoot.get("status"), "CONFIRMED"),
                            cb.between(bookingRoot.get("startTime"), 
                                date.atStartOfDay(), 
                                date.plusDays(1).atStartOfDay())
                        ));
                return cb.not(cb.exists(subquery));
            });
        }

        return courtRepository.findAll(spec, pageable);
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    private void mapToEntity(CourtRequest request, Court court) {
        court.setName(request.getName());
        court.setAddress(request.getAddress());
        court.setDescription(request.getDescription());
        court.setCourtType(request.getCourtType());
        court.setHourlyPrice(new BigDecimal(request.getHourlyPrice()));
    }

    public boolean isCourtOwner(Long courtId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserEmail = authentication.getName();
        Court court = getCourtById(courtId);
        return court.getOwner().getEmail().equals(currentUserEmail);
    }

    @Override
    public CourtRevenueResponse getCourtRevenue(Long courtId) {
        Court court = getCourtById(courtId);
        List<Booking> bookings = bookingRepository.findByCourtId(courtId);
        
        BigDecimal totalRevenue = bookings.stream()
            .filter(b -> "PAID".equals(b.getPaymentStatus()))
            .map(b -> BigDecimal.valueOf(b.getTotalPrice()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
            
        int totalBookings = bookings.size();
        int totalHoursBooked = bookings.stream()
            .mapToInt(b -> (int) java.time.Duration.between(b.getStartTime(), b.getEndTime()).toHours())
            .sum();
            
        return CourtRevenueResponse.builder()
            .courtId(court.getId())
            .courtName(court.getName())
            .totalRevenue(totalRevenue)
            .totalBookings(totalBookings)
            .totalHoursBooked(totalHoursBooked)
            .build();
    }

    @Override
    public List<CourtRevenueResponse> getAllCourtsRevenue() {
        List<Court> courts = courtRepository.findAll();
        return courts.stream()
            .map(court -> getCourtRevenue(court.getId()))
            .toList();
    }

    @Override
    public OwnerRevenueResponse getOwnerRevenue(Long ownerId) {
        List<Court> ownerCourts = courtRepository.findByOwnerId(ownerId);
        if (ownerCourts.isEmpty()) {
            throw new EntityNotFoundException("No courts found for owner with ID: " + ownerId);
        }

        BigDecimal totalRevenue = BigDecimal.ZERO;
        int totalBookings = 0;
        int totalHoursBooked = 0;

        for (Court court : ownerCourts) {
            CourtRevenueResponse courtRevenue = getCourtRevenue(court.getId());
            totalRevenue = totalRevenue.add(courtRevenue.getTotalRevenue());
            totalBookings += courtRevenue.getTotalBookings();
            totalHoursBooked += courtRevenue.getTotalHoursBooked();
        }

        User owner = ownerCourts.get(0).getOwner();
        return OwnerRevenueResponse.builder()
            .ownerId(owner.getId())
            .ownerName(owner.getFullName())
            .totalRevenue(totalRevenue)
            .totalCourts(ownerCourts.size())
            .totalBookings(totalBookings)
            .totalHoursBooked(totalHoursBooked)
            .build();
    }

    @Override
    public List<OwnerRevenueResponse> getAllOwnersRevenue() {
        List<User> owners = userRepository.findByRole(UserRole.OWNER);
        return owners.stream()
            .map(owner -> getOwnerRevenue(owner.getId()))
            .toList();
    }

    @Override
    public List<MonthlyRevenueResponse> getMonthlyRevenue(Long courtId) {
        Court court = getCourtById(courtId);
        LocalDateTime sixMonthsAgo = LocalDateTime.now().minusMonths(6);
        List<Booking> bookings = bookingRepository.findByCourtId(courtId).stream()
            .filter(b -> b.getStartTime().isAfter(sixMonthsAgo) && "PAID".equals(b.getPaymentStatus()))
            .toList();

        return calculateMonthlyRevenue(bookings);
    }

    @Override
    public List<MonthlyRevenueResponse> getOwnerMonthlyRevenue(Long ownerId) {
        List<Court> ownerCourts = courtRepository.findByOwnerId(ownerId);
        if (ownerCourts.isEmpty()) {
            throw new EntityNotFoundException("No courts found for owner with ID: " + ownerId);
        }

        LocalDateTime sixMonthsAgo = LocalDateTime.now().minusMonths(6);
        List<Booking> allBookings = new ArrayList<>();
        
        for (Court court : ownerCourts) {
            List<Booking> courtBookings = bookingRepository.findByCourtId(court.getId()).stream()
                .filter(b -> b.getStartTime().isAfter(sixMonthsAgo) && "PAID".equals(b.getPaymentStatus()))
                .toList();
            allBookings.addAll(courtBookings);
        }

        return calculateMonthlyRevenue(allBookings);
    }

    @Override
    public List<MonthlyRevenueResponse> getAllCourtsMonthlyRevenue() {
        LocalDateTime sixMonthsAgo = LocalDateTime.now().minusMonths(6);
        List<Booking> allBookings = bookingRepository.findAll().stream()
            .filter(b -> b.getStartTime().isAfter(sixMonthsAgo) && "PAID".equals(b.getPaymentStatus()))
            .toList();

        return calculateMonthlyRevenue(allBookings);
    }

    private List<MonthlyRevenueResponse> calculateMonthlyRevenue(List<Booking> bookings) {
        // Get current month and 5 months before
        YearMonth currentMonth = YearMonth.now();
        List<YearMonth> lastSixMonths = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            lastSixMonths.add(currentMonth.minusMonths(i));
        }

        // Group bookings by month
        Map<YearMonth, List<Booking>> bookingsByMonth = bookings.stream()
            .collect(Collectors.groupingBy(b -> YearMonth.from(b.getStartTime())));

        // Create response for each month
        return lastSixMonths.stream()
            .map(month -> {
                List<Booking> monthBookings = bookingsByMonth.getOrDefault(month, Collections.emptyList());
                BigDecimal totalRevenue = monthBookings.stream()
                    .map(b -> BigDecimal.valueOf(b.getTotalPrice()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                
                int totalHoursBooked = monthBookings.stream()
                    .mapToInt(b -> (int) java.time.Duration.between(b.getStartTime(), b.getEndTime()).toHours())
                    .sum();

                return MonthlyRevenueResponse.builder()
                    .month(month)
                    .totalRevenue(totalRevenue)
                    .totalHoursBooked(totalHoursBooked)
                    .totalBookings(monthBookings.size())
                    .build();
            })
            .toList();
    }

    @Override
    public List<TopCustomerResponse> getTopCustomers(Long ownerId) {
        List<Court> ownerCourts = courtRepository.findByOwnerId(ownerId);
        if (ownerCourts.isEmpty()) {
            throw new EntityNotFoundException("No courts found for owner with ID: " + ownerId);
        }

        // Get all bookings for owner's courts
        List<Booking> allBookings = new ArrayList<>();
        for (Court court : ownerCourts) {
            List<Booking> courtBookings = bookingRepository.findByCourtId(court.getId()).stream()
                .filter(b -> "PAID".equals(b.getPaymentStatus()))
                .toList();
            allBookings.addAll(courtBookings);
        }

        // Group bookings by customer
        Map<User, List<Booking>> bookingsByCustomer = allBookings.stream()
            .collect(Collectors.groupingBy(Booking::getUser));

        // Calculate statistics for each customer
        List<TopCustomerResponse> customerStats = bookingsByCustomer.entrySet().stream()
            .map(entry -> {
                User customer = entry.getKey();
                List<Booking> customerBookings = entry.getValue();

                int totalBookings = customerBookings.size();
                int totalHoursBooked = customerBookings.stream()
                    .mapToInt(b -> (int) java.time.Duration.between(b.getStartTime(), b.getEndTime()).toHours())
                    .sum();
                BigDecimal totalSpent = customerBookings.stream()
                    .map(b -> BigDecimal.valueOf(b.getTotalPrice()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                LocalDateTime lastBookingDate = customerBookings.stream()
                    .map(Booking::getStartTime)
                    .max(LocalDateTime::compareTo)
                    .orElse(null);

                return TopCustomerResponse.builder()
                    .customerId(customer.getId())
                    .customerName(customer.getFullName())
                    .customerEmail(customer.getEmail())
                    .customerPhone(customer.getPhoneNumber())
                    .totalBookings(totalBookings)
                    .totalHoursBooked(totalHoursBooked)
                    .totalSpent(totalSpent)
                    .lastBookingDate(lastBookingDate)
                    .build();
            })
            .sorted((c1, c2) -> c2.getTotalSpent().compareTo(c1.getTotalSpent())) // Sort by total spent in descending order
            .limit(5) // Get top 5 customers
            .toList();

        return customerStats;
    }
}
