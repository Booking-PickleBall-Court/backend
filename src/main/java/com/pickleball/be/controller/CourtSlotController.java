package com.pickleball.be.controller;

import com.pickleball.be.dto.CreateRecurringCourtSlotDTO;
import com.pickleball.be.dto.CourtSlotDTO;
import com.pickleball.be.service.CourtSlotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/court-slots")
public class CourtSlotController {

    @Autowired
    private CourtSlotService courtSlotService;

    @PostMapping
    public ResponseEntity<CourtSlotDTO> createCourtSlot(@RequestBody CourtSlotDTO courtSlotDTO) {
        if (courtSlotDTO.getDate() == null) {
            throw new IllegalArgumentException("Date cannot be null");
        }
        return ResponseEntity.ok(courtSlotService.createCourtSlot(courtSlotDTO));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CourtSlotDTO> getCourtSlot(@PathVariable Long id) {
        return ResponseEntity.ok(courtSlotService.getCourtSlot(id));
    }

    @GetMapping
    public ResponseEntity<List<CourtSlotDTO>> getAllCourtSlots() {
        return ResponseEntity.ok(courtSlotService.getAllCourtSlots());
    }

    @GetMapping("/court/{courtId}")
    public ResponseEntity<List<CourtSlotDTO>> getCourtSlotsByCourt(@PathVariable Long courtId) {
        return ResponseEntity.ok(courtSlotService.getCourtSlotsByCourt(courtId));
    }

    @GetMapping("/available")
    public ResponseEntity<List<CourtSlotDTO>> getAvailableCourtSlots() {
        return ResponseEntity.ok(courtSlotService.getAvailableCourtSlots());
    }

    @PutMapping("/{id}")
    public ResponseEntity<CourtSlotDTO> updateCourtSlot(@PathVariable Long id, @RequestBody CourtSlotDTO courtSlotDTO) {
        return ResponseEntity.ok(courtSlotService.updateCourtSlot(id, courtSlotDTO));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCourtSlot(@PathVariable Long id) {
        courtSlotService.deleteCourtSlot(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/recurring")
    public ResponseEntity<List<CourtSlotDTO>> createRecurringCourtSlots(@RequestBody CreateRecurringCourtSlotDTO dto) {
        if (dto.getStartDate() == null || dto.getEndDate() == null) {
            throw new IllegalArgumentException("Start date and end date cannot be null");
        }
        if (dto.getStartTime() == null || dto.getEndTime() == null) {
            throw new IllegalArgumentException("Start time and end time cannot be null");
        }
        return ResponseEntity.ok(courtSlotService.createRecurringCourtSlots(dto));
    }
} 