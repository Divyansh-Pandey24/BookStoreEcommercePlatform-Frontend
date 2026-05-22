package com.booknest.admin;

import de.codecentric.boot.admin.server.config.AdminServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
public class AdminSecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, AdminServerProperties adminServerProperties)
            throws Exception {
        String adminContextPath = adminServerProperties.getContextPath();

        // Saved request handler to redirect back to bookmarked pages on successful
        // login
        SavedRequestAwareAuthenticationSuccessHandler successHandler = new SavedRequestAwareAuthenticationSuccessHandler();
        successHandler.setTargetUrlParameter("redirectTo");
        successHandler.setDefaultTargetUrl(adminContextPath + "/");

        http
                .authorizeHttpRequests(auth -> auth
                        // Allow static assets, login page, and actuators
                        .requestMatchers(new AntPathRequestMatcher(adminContextPath + "/assets/**")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher(adminContextPath + "/login")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/actuator/**")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher(adminContextPath + "/actuator/**")).permitAll()
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage(adminContextPath + "/login")
                        .successHandler(successHandler))
                .logout(logout -> logout
                        .logoutUrl(adminContextPath + "/logout"))
                .csrf(csrf -> csrf.disable());

        return http.build();
    }
}
