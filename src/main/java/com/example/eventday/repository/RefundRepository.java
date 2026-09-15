package com.example.eventday.repository;

import com.example.eventday.entity.RefundRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface RefundRepository extends JpaRepository<RefundRequestEntity, UUID> {
}