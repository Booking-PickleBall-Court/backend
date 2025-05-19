package com.pickleball.be.model;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Entity
@Table(name = "court_statistics")
public class CourtStatistics {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "court_id", nullable = false)
    private Court court;

    @Column(nullable = false)
    private String month;

    @Column(name = "total_revenue", nullable = false)
    private BigDecimal totalRevenue;

    @Column(name = "total_hours_booked", nullable = false)
    private Integer totalHoursBooked;

    @ManyToOne
    @JoinColumn(name = "top_client_id")
    private User topClient;
} 