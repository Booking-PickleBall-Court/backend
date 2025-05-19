package com.pickleball.be.service.impl;

import com.pickleball.be.dto.court.CourtRequest;
import com.pickleball.be.model.*;
import com.pickleball.be.repository.CourtRepository;
import com.pickleball.be.repository.UserRepository;
import com.pickleball.be.service.CourtService;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CourtServiceImpl implements CourtService {

    private final CourtRepository courtRepository;
    private final UserRepository userRepository;

    @Override
    public Court createCourt(CourtRequest request) {
        User currentOwner = getCurrentUser();
        Court court = new Court();
        mapToEntity(request, court);
        court.setOwner(currentOwner);
        court.setStatus(CourtStatus.AVAILABLE);
        return courtRepository.save(court);
    }

    @Override
    public Court updateCourt(Long id, CourtRequest request) {
        Court court = getCourtById(id);
        mapToEntity(request, court);
        return courtRepository.save(court);
    }

    @Override
    public void deleteCourt(Long id) {
        courtRepository.deleteById(id);
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
                var slotRoot = subquery.from(CourtSlot.class);
                subquery.select(slotRoot.get("court").get("id"))
                        .where(cb.and(
                            cb.equal(slotRoot.get("court"), root),
                            cb.equal(slotRoot.get("date"), date),
                            cb.equal(slotRoot.get("status"), "AVAILABLE"),
                            cb.isTrue(slotRoot.get("isAvailable"))
                        ));
                return cb.exists(subquery);
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
        court.setImageUrl("");
        court.setHourlyPrice(request.getHourlyPrice());
    }
}
