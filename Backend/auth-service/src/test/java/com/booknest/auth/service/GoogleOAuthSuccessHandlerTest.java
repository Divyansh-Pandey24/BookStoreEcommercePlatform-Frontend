package com.booknest.auth.service;

import com.booknest.auth.entity.User;
import com.booknest.auth.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.io.IOException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GoogleOAuthSuccessHandlerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private GoogleOAuthSuccessHandler successHandler;

    private User mockUser;

    @BeforeEach
    void setUp() {
        org.springframework.test.util.ReflectionTestUtils.setField(successHandler, "frontendUrl", "http:// localhost:5173");

        mockUser = new User();
        mockUser.setUserId(1L);
        mockUser.setEmail("test@booknest.com");
        mockUser.setFullName("Test User");
        mockUser.setRole("CUSTOMER");
        mockUser.setProvider("LOCAL");
        mockUser.setProfilePicture("http:// old-pic");
    }

    @Test
    void onAuthenticationSuccess_oidcUserExisting_redirectsWithTokens() throws IOException {
        OidcUser oidcUser = mock(OidcUser.class);
        when(oidcUser.getEmail()).thenReturn("test@booknest.com");
        when(oidcUser.getFullName()).thenReturn("Test User");
        when(oidcUser.getPicture()).thenReturn("http:// new-pic");

        when(authentication.getPrincipal()).thenReturn(oidcUser);
        when(userRepository.findByEmail("test@booknest.com")).thenReturn(Optional.of(mockUser));

        when(jwtService.generateAccessToken(mockUser)).thenReturn("g-access");
        when(jwtService.generateRefreshToken(mockUser)).thenReturn("g-refresh");

        successHandler.onAuthenticationSuccess(request, response, authentication);

        ArgumentCaptor<String> redirectCaptor = ArgumentCaptor.forClass(String.class);
        verify(response).sendRedirect(redirectCaptor.capture());

        String redirectUrl = redirectCaptor.getValue();
        assertThat(redirectUrl).contains("http:// localhost:5173/oauth2/success");
        assertThat(redirectUrl).contains("accessToken=g-access");
        // email is NOT passed through encode() in the handler it's placed raw in the URL
        assertThat(redirectUrl).contains("email=test@booknest.com");
    }

    @Test
    void onAuthenticationSuccess_oauth2UserNew_createsAndRedirects() throws IOException {
        OAuth2User oauth2User = mock(OAuth2User.class);
        when(oauth2User.getAttribute("email")).thenReturn("new@booknest.com");
        when(oauth2User.getAttribute("name")).thenReturn("New User");
        when(oauth2User.getAttribute("picture")).thenReturn("http:// pic");

        when(authentication.getPrincipal()).thenReturn(oauth2User);
        when(userRepository.findByEmail("new@booknest.com")).thenReturn(Optional.empty());

        User savedUser = new User();
        savedUser.setUserId(2L);
        savedUser.setEmail("new@booknest.com");
        savedUser.setFullName("New User");
        savedUser.setProvider("GOOGLE");
        savedUser.setRole("CUSTOMER");
        savedUser.setProfilePicture("http:// pic");

        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtService.generateAccessToken(savedUser)).thenReturn("new-access");
        when(jwtService.generateRefreshToken(savedUser)).thenReturn("new-refresh");

        successHandler.onAuthenticationSuccess(request, response, authentication);

        verify(userRepository, times(2)).save(any(User.class));
        verify(response).sendRedirect(anyString());
    }

    @Test
    void onAuthenticationSuccess_noEmail_redirectsWithError() throws IOException {
        OidcUser oidcUser = mock(OidcUser.class);
        when(oidcUser.getEmail()).thenReturn(null);

        when(authentication.getPrincipal()).thenReturn(oidcUser);

        successHandler.onAuthenticationSuccess(request, response, authentication);

        verify(response).sendRedirect("http:// localhost:5173/login?error=google_email_not_available");
        verify(userRepository, never()).findByEmail(anyString());
    }
}
