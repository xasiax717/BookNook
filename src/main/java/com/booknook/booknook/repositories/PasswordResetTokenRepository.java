package com.booknook.booknook.repositories;

import com.booknook.booknook.entities.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByToken(String token);
    void deleteByUser_Id(Long userId);
    Optional<PasswordResetToken> findByUserId(Long userId);
    void deleteByUserId(Long userId);
}