package com.booknest.order.client;

import com.booknest.order.dto.WalletDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

// ============================================================
// FALLBACK for order-service → WALLET-SERVICE calls.
//
// This is the most FINANCIALLY CRITICAL fallback in the system.
//
//   getWallet()   → Returns stub with 0 balance.
//                   OrderService will see "insufficient balance"
//                   and reject the order. Safe default.
//
//   deductMoney() → MUST THROW. If WALLET-SERVICE is down and we
//                   silently succeed here, the order is placed but
//                   money is never deducted — financial loss.
//                   Throwing forces the order to fail cleanly.
//
//   addMoney()    → Called to REFUND a failed order. If WALLET-SERVICE
//                   is down, the refund can't happen. Log a CRITICAL
//                   alert. Do NOT throw — the order is already being
//                   cancelled; throwing would mask the original error.
//                   Ops team must trigger manual refund.
// ============================================================
@Component
public class WalletClientFallback implements WalletClient {

    private static final Logger log = LoggerFactory.getLogger(WalletClientFallback.class);

    // Returns 0 balance — OrderService will reject "insufficient funds".
    // Better than assuming the user has money when we can't verify.
    @Override
    public WalletDto getWallet(Long userId) {
        log.warn("[CIRCUIT BREAKER] WALLET-SERVICE unavailable. " +
                 "Returning 0-balance WalletDto for userId={}. " +
                 "Order will be rejected (insufficient balance).", userId);
        WalletDto fallback = new WalletDto();
        fallback.setUserId(userId);
        // 0 balance → OrderService rejects the payment attempt
        fallback.setCurrentBalance(0.0);
        return fallback;
    }

    // CRITICAL: Cannot silently ignore a money deduction.
    // If we return here without throwing, the order gets placed
    // but no money is deducted — this is a financial bug.
    // We throw RuntimeException to abort the order transaction.
    @Override
    public void deductMoney(Long userId, Double amount) {
        log.error("[CIRCUIT BREAKER] WALLET-SERVICE unavailable. " +
                  "CRITICAL: Cannot deduct ₹{} from userId={}. " +
                  "Throwing exception to prevent order without payment.",
                  amount, userId);
        // This exception propagates up to OrderService and rolls back
        // the entire order placement — the correct behavior.
        throw new RuntimeException(
            "Payment service unavailable. Please try again in a few moments."
        );
    }

    // Called to REFUND money on order cancellation/failure.
    // WALLET-SERVICE is already down — we can't refund.
    // Log CRITICAL for ops team. Do NOT throw — we're already
    // in a failure path; throwing here would hide the root cause.
    @Override
    public void addMoney(Long userId, Double amount) {
        log.error("[CIRCUIT BREAKER] WALLET-SERVICE unavailable. " +
                  "CRITICAL ALERT: Could not refund ₹{} to userId={}. " +
                  "Manual refund required when WALLET-SERVICE recovers!",
                  amount, userId);
        // Return silently — refund must be done manually
    }
}
