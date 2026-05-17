package com.booknest.wallet.client;

import com.booknest.wallet.dto.UserProfileDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

// ============================================================
// FALLBACK for wallet-service → AUTH-SERVICE calls.
//
// getUserProfile() is used to enrich wallet responses with user
// display info (name, email). If AUTH-SERVICE is down, we return
// a stub profile — the wallet still works, just shows less info.
//
// WHY return stub instead of throw?
//   getUserProfile() is for DISPLAY only. The actual wallet
//   balance, deduct, add operations don't depend on it.
//   A stub profile is better than crashing the entire wallet page.
// ============================================================
@Component
public class UserClientFallback implements UserClient {

    private static final Logger log = LoggerFactory.getLogger(UserClientFallback.class);

    // Called when AUTH-SERVICE is DOWN or circuit is OPEN.
    // Returns a minimal stub so wallet display still works.
    @Override
    public UserProfileDto getUserProfile(Long userId) {
        log.warn("[CIRCUIT BREAKER] AUTH-SERVICE is unavailable. " +
                 "Returning fallback UserProfileDto for userId={}", userId);
        UserProfileDto fallback = new UserProfileDto();
        // Preserve the userId so the caller can still identify the wallet owner
        fallback.setUserId(userId);
        // Generic placeholder values — frontend should handle these gracefully
        fallback.setFullName("Unknown User");
        fallback.setEmail("unavailable@service.down");
        fallback.setMobile("N/A");
        return fallback;
    }
}
