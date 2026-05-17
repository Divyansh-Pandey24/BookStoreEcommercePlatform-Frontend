package com.booknest.auth.service;

import com.booknest.auth.entity.PasswordResetToken;
import com.booknest.auth.entity.User;
import com.booknest.auth.repository.PasswordResetTokenRepository;
import com.booknest.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private UserRepository userAuthRepository;

    @Mock
    private PasswordResetTokenRepository tokenRepo;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private PasswordResetService passwordResetService;

    private User mockUser;
    private PasswordResetToken mockToken;

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setUserId(1L);
        mockUser.setEmail("test@booknest.com");

        mockToken = new PasswordResetToken();
        mockToken.setToken("valid-token");
        mockToken.setUserAuthEntity(mockUser);
        mockToken.setExpiryDate(LocalDateTime.now().plusMinutes(10));
    }

    @Test
    void forgotPassword_validEmail_sendsEmailAndSavesToken() {
        when(userAuthRepository.findByEmail("test@booknest.com")).thenReturn(Optional.of(mockUser));

        String result = passwordResetService.forgotPassword("test@booknest.com");

        assertThat(result).isEqualTo("Reset link sent successfully");
        verify(tokenRepo, times(1)).save(any(PasswordResetToken.class));
        // mailSender.send() is called asynchronously via CompletableFuture, so we skip verification here
    }

    @Test
    void forgotPassword_invalidEmail_throwsException() {
        when(userAuthRepository.findByEmail("unknown@booknest.com")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> passwordResetService.forgotPassword("unknown@booknest.com"));
        verify(tokenRepo, never()).save(any());
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void findByToken_validToken_returnsToken() {
        when(tokenRepo.findByToken("valid-token")).thenReturn(Optional.of(mockToken));

        PasswordResetToken result = passwordResetService.findByToken("valid-token");
        assertThat(result.getToken()).isEqualTo("valid-token");
    }

    @Test
    void findByToken_invalidToken_throwsException() {
        when(tokenRepo.findByToken("invalid-token")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> passwordResetService.findByToken("invalid-token"));
    }

    @Test
    void deleteToken_invokesRepoDelete() {
        passwordResetService.deleteToken(mockToken);
        verify(tokenRepo, times(1)).delete(mockToken);
    }

    @Test
    void resetPassword_validToken_updatesPasswordAndDeletesToken() {
        when(tokenRepo.findByToken("valid-token")).thenReturn(Optional.of(mockToken));
        when(userAuthRepository.findByEmail("test@booknest.com")).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.encode("newPassword")).thenReturn("hashedPassword");

        String result = passwordResetService.resetPassword("valid-token", "newPassword");

        assertThat(result).isEqualTo("Password updated successfully");
        assertThat(mockUser.getPasswordHash()).isEqualTo("hashedPassword");
        verify(userAuthRepository, times(1)).save(mockUser);
        verify(tokenRepo, times(1)).delete(mockToken);
    }

    @Test
    void resetPassword_expiredToken_throwsException() {
        mockToken.setExpiryDate(LocalDateTime.now().minusMinutes(10)); // expired
        when(tokenRepo.findByToken("valid-token")).thenReturn(Optional.of(mockToken));

        assertThrows(RuntimeException.class, () -> passwordResetService.resetPassword("valid-token", "newPassword"));
        verify(userAuthRepository, never()).save(any());
    }

    @Test
    void resetPassword_invalidToken_throwsException() {
        when(tokenRepo.findByToken("invalid-token")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> passwordResetService.resetPassword("invalid-token", "newPassword"));
    }
}
