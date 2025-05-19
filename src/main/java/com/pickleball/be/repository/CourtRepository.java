package com.pickleball.be.repository;

import com.pickleball.be.model.Court;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourtRepository extends JpaRepository<Court, Long>, JpaSpecificationExecutor<Court> {
    List<Court> findByOwnerId(Long ownerId);
}