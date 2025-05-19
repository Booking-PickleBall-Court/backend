package com.pickleball.be.model;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Entity
@Table(name = "user_statistics")
public class UserStatistics {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String month;

    @Column(name = "total_spent", nullable = false)
    private BigDecimal totalSpent;

    @ManyToOne
    @JoinColumn(name = "top_court_id")
    private Court topCourt;
} 