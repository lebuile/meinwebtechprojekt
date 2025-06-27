package com.lengoga.webtech_projekt.repository;

import com.lengoga.webtech_projekt.model.entity.PasswordResetToken;
import com.lengoga.webtech_projekt.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    PasswordResetToken findByToken(String token);
    void deleteByUser(User user);
}
