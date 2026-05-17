package com.booknest.auth.service;

import com.booknest.auth.dto.AuthResponse;
import com.booknest.auth.entity.User;
import com.booknest.auth.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import org.springframework.beans.factory.annotation.Value;
import java.io.IOException;

// Handler for successful Google OAuth2 authentication
@Component
@RequiredArgsConstructor
@Slf4j
public class GoogleOAuthSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtService jwtService;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException {

        String email = null;
        String name = null;
        String picture = null;

        Object principal = authentication.getPrincipal();

        // Extract user details from OIDC or OAuth2 principal
        if (principal instanceof OidcUser oidcUser) {
            email = oidcUser.getEmail();
            name = oidcUser.getFullName();
            picture = oidcUser.getPicture();
            log.info("Google OIDC login: email={}, name={}", email, name);
        } else if (principal instanceof OAuth2User oAuth2User) {
            email = oAuth2User.getAttribute("email");
            name = oAuth2User.getAttribute("name");
            picture = oAuth2User.getAttribute("picture");
            log.info("Google OAuth2 login: email={}", email);
        }

        if (email == null) {
            log.error("Google OAuth: email is null");
            response.sendRedirect(this.frontendUrl + "/login?error=google_email_not_available");
            return;
        }

        final String finalEmail = email;
        final String finalName = (name != null ? name : "");
        final String finalPicture = picture;

        // Find or create user record
        User user = userRepository.findByEmail(finalEmail).orElseGet(() -> {
            User newUser = new User();
            newUser.setFullName(finalName);
            newUser.setEmail(finalEmail);
            newUser.setPasswordHash("GOOGLE_OAUTH_NO_PASSWORD");
            newUser.setRole("CUSTOMER");
            newUser.setProvider("GOOGLE");
            newUser.setProfilePicture(finalPicture);
            return userRepository.save(newUser);
        });

        if ("GOOGLE".equals(user.getProvider()) && finalPicture != null) {
            user.setProfilePicture(finalPicture);
            user = userRepository.save(user);
        }

        if (Boolean.TRUE.equals(user.getSuspended())) {
            log.warn("Suspended user attempted Google login: {}", user.getEmail());
            response.sendRedirect(this.frontendUrl + "/login?error=account_suspended");
            return;
        }

        // Generate response tokens
        AuthResponse authResponse = new AuthResponse(
                jwtService.generateAccessToken(user),
                jwtService.generateRefreshToken(user),
                user.getRole(),
                user.getUserId(),
                user.getEmail(),
                user.getFullName(),
                user.getProfilePicture());

        // Redirect to frontend with tokens in query parameters
        String redirectUrl = this.frontendUrl + "/oauth2/success"
                + "?accessToken=" + authResponse.getAccessToken()
                + "&refreshToken=" + authResponse.getRefreshToken()
                + "&role=" + authResponse.getRole()
                + "&userId=" + authResponse.getUserId()
                + "&email=" + authResponse.getEmail()
                + "&fullName=" + encode(authResponse.getFullName())
                + "&profilePicture=" + encode(authResponse.getProfilePicture());

        response.sendRedirect(redirectUrl);
    }

    // Helper to URL-encode values for query parameters
    private String encode(String value) {
        if (value == null)
            return "";
        try {
            return java.net.URLEncoder.encode(value, "UTF-8");
        } catch (Exception e) {
            return "";
        }
    }
}