package com.pickleball.be.service.impl;

import com.pickleball.be.dto.court.CourtRequest;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
}
