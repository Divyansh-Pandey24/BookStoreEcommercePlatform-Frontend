package com.booknest.order.client;

import com.booknest.order.dto.UserProfileDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

// ============================================================
// FALLBACK for order-service → AUTH-SERVICE calls.
//
// getUserProfile() is used to enrich order confirmation emails
// and order responses with user display info (name, email).
// It is NOT involved in stock reservation or payment.
//
// If AUTH-SERVICE is down:
//   - Return a stub with placeholder name/email
//   - The order can still be placed (stub is display-only)
//   - The confirmation email might show "Unknown User" but
//     that's far better than blocking the entire order flow
// ============================================================
@Component
public class UserClientFallback implements UserClient {

    private static final Logger log = LoggerFactory.getLogger(UserClientFallback.class);

    // Called when AUTH-SERVICE is DOWN or circuit is OPEN.
    // Returns a stub profile. The order still proceeds normally.
    // Email notifications will show placeholder name/email.
    @Override
    public UserProfileDto getUserProfile(Long userId) {
        log.warn("[CIRCUIT BREAKER] AUTH-SERVICE unavailable. " +
                 "Returning fallback UserProfileDto for userId={}", userId);
        UserProfileDto fallback = new UserProfileDto();
        // Preserve userId so the order can still be linked to the user
        fallback.setUserId(userId);
        // Placeholder values for display purposes only
        fallback.setFullName("Unknown User");
        fallback.setEmail("unavailable@service.down");
        fallback.setMobile("N/A");
        return fallback;
    }
}
