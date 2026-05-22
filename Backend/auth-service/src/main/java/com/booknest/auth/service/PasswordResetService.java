package com.booknest.auth.service;

import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.booknest.auth.entity.PasswordResetToken;
import com.booknest.auth.entity.User;
import com.booknest.auth.repository.PasswordResetTokenRepository;
import com.booknest.auth.repository.UserRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// Service handle password reset functionality
@Slf4j
@Service
@AllArgsConstructor
public class PasswordResetService {

    private JavaMailSender mailSender;
    private UserRepository userAuthRepository;
    private PasswordResetTokenRepository tokenRepo;
    private PasswordEncoder passwordEncoder;

    // Initiates the forgot password flow by sending a reset link
    public String forgotPassword(String email) {
        log.info("Initiating password reset for email: {}", email);
        User user = userAuthRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Delete any existing tokens for this user to avoid unique constraint violation
        tokenRepo.deleteByUserAuthEntity(user);

        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken(token);
        resetToken.setUserAuthEntity(user);
        resetToken.setExpiryDate(LocalDateTime.now().plusMinutes(15));
        tokenRepo.save(resetToken);

        // Build dynamic link based on environment
        String frontendUrl = System.getenv("FRONTEND_URL");
        if (frontendUrl == null || frontendUrl.isBlank()) {
            frontendUrl = "http://localhost:5173"; // default for local testing
        }
        String resetLink = frontendUrl + "/reset-password?token=" + token;
        sendEmail(user.getEmail(), resetLink);
        return "Reset link sent successfully";
    }

    // Helper to send the reset email
    private void sendEmail(String to, String link) {
        log.info("Sending password reset email to: {}", to);
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setTo(to);
                message.setSubject("Reset Password");
                message.setText("Click here to reset your password: " + link);
                mailSender.send(message);
                log.info("Password reset email sent successfully to: {}", to);
            } catch (Exception e) {
                log.error("Failed to send password reset email to: {}", to, e);
            }
        });
    }

    // Finds a token entity by its string value
    public PasswordResetToken findByToken(String token) {
        return tokenRepo.findByToken(token)
                .orElseThrow(() -> new RuntimeException("The token was expired or invalid"));
    }

    // Deletes a password reset token
    public void deleteToken(PasswordResetToken token) {
        tokenRepo.delete(token);
    }

    // Resets the user's password using a valid token
    public String resetPassword(String token, String newPassword) {
        log.info("Processing password reset with token");
        PasswordResetToken resetToken = tokenRepo.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid token"));

        if (resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Token expired");
        }

        User user = userAuthRepository.findByEmail(resetToken.getUserAuthEntity().getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userAuthRepository.save(user);
        tokenRepo.delete(resetToken);
        return "Password updated successfully";
    }
}
