package com.example.eventday.repository;
import java.util.UUID;
import com.example.eventday.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
// UserRepository.java
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByNik(String nik);
}