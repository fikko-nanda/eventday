package com.example.eventday.repository;

import com.example.eventday.entity.Auth;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AuthRepository extends JpaRepository<Auth, UUID> {
    Optional<Auth> findByUserUserId(UUID userId);
    Optional<Auth> findByAksesToken(String aksesToken);
}