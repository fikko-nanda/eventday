package com.example.eventday.repository;

import com.example.eventday.entity.Otp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OtpRepository extends JpaRepository<Otp, UUID> {
    Optional<Otp> findByUserUserIdAndOtpCode(UUID userId, String otpCode);
    Optional<Otp> findByUserUserId(UUID userId);
    void deleteByUserUserId(UUID userId);
}
