package com.booknest.wallet.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import com.booknest.wallet.dto.UserProfileDto;

// Feign client for auth-service.
// fallback = UserClientFallback.class:
//   Returns stub UserProfile — wallet still works, just shows "Unknown User".
@FeignClient(name = "AUTH-SERVICE", fallback = UserClientFallback.class)
public interface UserClient {

    // Retrieve user profile
    @GetMapping("/auth/user/{userId}")
    UserProfileDto getUserProfile(@PathVariable Long userId);
}