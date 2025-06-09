package com.pickleball.be.repository;

import com.pickleball.be.model.SubCourt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SubCourtRepository extends JpaRepository<SubCourt, Long> {
} 