package com.pickleball.be.dto.court;

import com.pickleball.be.model.CourtType;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import com.pickleball.be.dto.court.SubCourtRequest;

@Data
public class CourtRequest {
    private String name;
    private String address;
    private String description;
    private String hourlyPrice;
    private CourtType courtType;
    private List<MultipartFile> images;
    private List<SubCourtRequest> subCourts;
} 