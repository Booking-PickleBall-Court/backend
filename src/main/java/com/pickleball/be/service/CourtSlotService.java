package com.pickleball.be.service;

import com.pickleball.be.dto.CreateRecurringCourtSlotDTO;
import com.pickleball.be.dto.CourtSlotDTO;
import com.pickleball.be.model.Court;
import com.pickleball.be.model.CourtSlot;
import com.pickleball.be.repository.CourtRepository;
import com.pickleball.be.repository.CourtSlotRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CourtSlotService {

    @Autowired
    private CourtSlotRepository courtSlotRepository;

    @Autowired
    private CourtRepository courtRepository;

    public CourtSlotDTO createCourtSlot(CourtSlotDTO courtSlotDTO) {
        Court court = courtRepository.findById(courtSlotDTO.getCourtId())
                .orElseThrow(() -> new EntityNotFoundException("Court not found"));

        CourtSlot courtSlot = new CourtSlot();
        courtSlot.setCourt(court);
        courtSlot.setDate(courtSlotDTO.getDate());
        courtSlot.setStartTime(courtSlotDTO.getStartTime());
        courtSlot.setEndTime(courtSlotDTO.getEndTime());
        courtSlot.setAvailable(courtSlotDTO.isAvailable());
        courtSlot.setPrice(courtSlotDTO.getPrice());
        courtSlot.setStatus(courtSlotDTO.getStatus());

        CourtSlot savedSlot = courtSlotRepository.save(courtSlot);
        return convertToDTO(savedSlot);
    }

    public CourtSlotDTO getCourtSlot(Long id) {
        CourtSlot courtSlot = courtSlotRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Court slot not found"));
        return convertToDTO(courtSlot);
    }

    public List<CourtSlotDTO> getAllCourtSlots() {
        return courtSlotRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<CourtSlotDTO> getCourtSlotsByCourt(Long courtId) {
        return courtSlotRepository.findByCourtId(courtId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<CourtSlotDTO> getAvailableCourtSlots() {
        return courtSlotRepository.findByIsAvailableTrue().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public CourtSlotDTO updateCourtSlot(Long id, CourtSlotDTO courtSlotDTO) {
        CourtSlot courtSlot = courtSlotRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Court slot not found"));

        if (courtSlotDTO.getCourtId() != null) {
            Court court = courtRepository.findById(courtSlotDTO.getCourtId())
                    .orElseThrow(() -> new EntityNotFoundException("Court not found"));
            courtSlot.setCourt(court);
        }
        if (courtSlotDTO.getDate() != null) {
            courtSlot.setDate(courtSlotDTO.getDate());
        }
        if (courtSlotDTO.getStartTime() != null) {
            courtSlot.setStartTime(courtSlotDTO.getStartTime());
        }
        if (courtSlotDTO.getEndTime() != null) {
            courtSlot.setEndTime(courtSlotDTO.getEndTime());
        }
        courtSlot.setAvailable(courtSlotDTO.isAvailable());
        if (courtSlotDTO.getPrice() != null) {
            courtSlot.setPrice(courtSlotDTO.getPrice());
        }
        if (courtSlotDTO.getStatus() != null) {
            courtSlot.setStatus(courtSlotDTO.getStatus());
        }

        CourtSlot updatedSlot = courtSlotRepository.save(courtSlot);
        return convertToDTO(updatedSlot);
    }

    public void deleteCourtSlot(Long id) {
        if (!courtSlotRepository.existsById(id)) {
            throw new EntityNotFoundException("Court slot not found");
        }
        courtSlotRepository.deleteById(id);
    }

    @Transactional
    public List<CourtSlotDTO> createRecurringCourtSlots(CreateRecurringCourtSlotDTO dto) {
        Court court = courtRepository.findById(dto.getCourtId())
                .orElseThrow(() -> new EntityNotFoundException("Court not found"));

        List<CourtSlot> slots = new ArrayList<>();
        LocalDate currentDate = dto.getStartDate();

        while (!currentDate.isAfter(dto.getEndDate())) {
            CourtSlot slot = new CourtSlot();
            slot.setCourt(court);
            slot.setDate(currentDate);
            
            // Set start time for the current date
            LocalDateTime startDateTime = LocalDateTime.of(
                currentDate.getYear(),
                currentDate.getMonthValue(),
                currentDate.getDayOfMonth(),
                dto.getStartTime().getHour(),
                dto.getStartTime().getMinute(),
                dto.getStartTime().getSecond()
            );
            slot.setStartTime(startDateTime);
            
            // Set end time for the current date
            LocalDateTime endDateTime = LocalDateTime.of(
                currentDate.getYear(),
                currentDate.getMonthValue(),
                currentDate.getDayOfMonth(),
                dto.getEndTime().getHour(),
                dto.getEndTime().getMinute(),
                dto.getEndTime().getSecond()
            );
            slot.setEndTime(endDateTime);
            
            slot.setAvailable(true);
            slot.setPrice(dto.getPrice());
            slot.setStatus(dto.getStatus() != null ? dto.getStatus() : "AVAILABLE");
            
            slots.add(slot);
            currentDate = currentDate.plusDays(1);
        }

        List<CourtSlot> savedSlots = courtSlotRepository.saveAll(slots);
        return savedSlots.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private CourtSlotDTO convertToDTO(CourtSlot courtSlot) {
        CourtSlotDTO dto = new CourtSlotDTO();
        dto.setId(courtSlot.getId());
        dto.setCourtId(courtSlot.getCourt().getId());
        dto.setDate(courtSlot.getDate());
        dto.setStartTime(courtSlot.getStartTime());
        dto.setEndTime(courtSlot.getEndTime());
        dto.setAvailable(courtSlot.isAvailable());
        dto.setPrice(courtSlot.getPrice());
        dto.setStatus(courtSlot.getStatus());
        return dto;
    }
} 