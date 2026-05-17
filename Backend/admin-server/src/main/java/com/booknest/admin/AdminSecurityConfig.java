package com.booknest.admin;

import de.codecentric.boot.admin.server.config.AdminServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

// ============================================================
// WHY THIS CLASS EXISTS:
//   Spring Boot Admin Server's UI is a single-page React app.
//   The default Spring Security config blocks its assets and
//   the /instances endpoint (used by Actuator clients to register).
//
//   This config does three things:
//   1. Allows the Admin UI assets to load (CSS, JS, images)
//   2. Allows Actuator clients to POST to /instances to register
//   3. Protects everything else with form-based login
// ============================================================
@Configuration
@EnableWebSecurity
public class AdminSecurityConfig {

    // contextPath is set in application.properties (default is "").
    // We inject it so our security rules are always path-correct.
    private final String adminContextPath;

    public AdminSecurityConfig(AdminServerProperties adminServerProperties) {
        // Read the admin context path from AdminServerProperties bean.
        // This ensures our security matchers are correct even if you
        // later change the context path in application.properties.
        this.adminContextPath = adminServerProperties.getContextPath();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        // When login succeeds, redirect to Admin UI dashboard home.
        SavedRequestAwareAuthenticationSuccessHandler successHandler =
            new SavedRequestAwareAuthenticationSuccessHandler();
        successHandler.setTargetUrlParameter("redirectTo");
        // After login, send user to the Admin dashboard root.
        successHandler.setDefaultTargetUrl(adminContextPath + "/");

        http
            // ---- AUTHORISATION RULES ----
            .authorizeHttpRequests(auth -> auth
                // Allow admin UI static assets: CSS, JS, images, fonts.
                // These are served by the Admin Server itself.
                // Without this, the browser gets 403 for every asset.
                .requestMatchers(new AntPathRequestMatcher(adminContextPath + "/assets/**")).permitAll()

                // Allow the login page itself (obviously must be accessible).
                .requestMatchers(new AntPathRequestMatcher(adminContextPath + "/login")).permitAll()

                // Allow actuator clients (the microservices) to POST /instances
                // to register themselves and to send heartbeats.
                // Without permitAll() here, services get 401 when registering.
                .requestMatchers(new AntPathRequestMatcher("/actuator/**")).permitAll()

                // Allow the Admin Server's own health endpoint to be public.
                // This is used by Eureka to check if admin-server is alive.
                .requestMatchers(new AntPathRequestMatcher(adminContextPath + "/actuator/**")).permitAll()

                // Everything else (the dashboard, metrics, logs, env, etc.)
                // requires the user to be authenticated.
                .anyRequest().authenticated()
            )

            // ---- FORM LOGIN ----
            // Uses Spring Security's built-in login page.
            // Credentials are set in application.properties:
            //   spring.security.user.name=admin
            //   spring.security.user.password=admin123
            .formLogin(form -> form
                .loginPage(adminContextPath + "/login")    // Show our login page at /login
                .successHandler(successHandler)            // Redirect to dashboard after login
            )

            // ---- LOGOUT ----
            // POST /logout clears the session and redirects to /login.
            .logout(logout -> logout
                .logoutUrl(adminContextPath + "/logout")
            )

            // ---- CSRF ----
            // Spring Boot Admin UI uses CSRF tokens stored in cookies.
            // CookieCsrfTokenRepository.withHttpOnlyFalse() allows
            // the Admin UI's JavaScript to read the CSRF cookie
            // and attach it to API requests.
            // Without this, every mutating API call (POST, DELETE) fails.
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                // Exclude /instances and /actuator from CSRF because
                // microservices use HTTP (not browser), so no CSRF cookie.
                .ignoringRequestMatchers(
                    new AntPathRequestMatcher(adminContextPath + "/instances"),
                    new AntPathRequestMatcher(adminContextPath + "/actuator/**")
                )
            );

        return http.build();
    }
}
