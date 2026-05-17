package com.booknest.wallet.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.booknest.wallet.dto.PaymentVerifyRequest;
import com.booknest.wallet.dto.RazorpayOrderRequest;
import com.booknest.wallet.dto.RazorpayOrderResponse;
import com.booknest.wallet.dto.WalletDto;
import com.booknest.wallet.entity.Transaction;
import com.booknest.wallet.service.WalletService;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

// Controller for managing user wallets, transactions, and payment gateway integration
@Slf4j
@RestController
@RequestMapping("/wallet")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    // Retrieve wallet details for a specific user ID (Internal)
    @GetMapping("/{userId}")
    public WalletDto getWallet(@PathVariable Long userId) {
        log.info("Fetching wallet for user ID: {}", userId);
        return walletService.getWallet(userId);
    }

    // Retrieve wallet details for the authenticated user
    @GetMapping
    public ResponseEntity<WalletDto> getMyWallet(@RequestHeader("X-User-Id") Long userId) {
        log.info("Fetching current user wallet: {}", userId);
        return ResponseEntity.ok(walletService.getWallet(userId));
    }

    // List all transactions for the authenticated user
    @GetMapping("/transactions")
    public ResponseEntity<List<Transaction>> getMyTransactions(@RequestHeader("X-User-Id") Long userId) {
        log.info("Fetching transactions for user: {}", userId);
        return ResponseEntity.ok(walletService.getTransactions(userId));
    }

    @Value("${gateway.secret}")
    private String gatewaySecret;

    // Deduct funds from a user's wallet for an order (INTERNAL ONLY)
    @PostMapping("/{userId}/deduct")
    public ResponseEntity<Void> deductMoney(@RequestHeader("X-Gateway-Secret") String secret, @PathVariable Long userId, @RequestParam Double amount, @RequestParam(required = false) Long orderId) {
        if (!gatewaySecret.equals(secret)) {
            log.warn("Unauthorized attempt to deduct money from user: {}", userId);
            return ResponseEntity.status(403).build();
        }
        log.info("Deducting {} from wallet {} for order {}", amount, userId, orderId);
        walletService.deductMoney(userId, amount, orderId);
        return ResponseEntity.ok().build();
    }

    // Refund funds back to a user's wallet (INTERNAL ONLY)
    @PostMapping("/{userId}/add")
    public ResponseEntity<Void> refundMoney(@RequestHeader("X-Gateway-Secret") String secret, @PathVariable Long userId, @RequestParam Double amount, @RequestParam(required = false) Long orderId) {
        if (!gatewaySecret.equals(secret)) {
            log.warn("Unauthorized attempt to add money to user: {}", userId);
            return ResponseEntity.status(403).build();
        }
        log.info("Refunding {} to wallet {} for order {}", amount, userId, orderId);
        walletService.addMoney(userId, amount, orderId);
        return ResponseEntity.ok().build();
    }

    // Initialize a Razorpay payment order
    @PostMapping("/razorpay/create-order")
    public ResponseEntity<RazorpayOrderResponse> createOrder(@RequestHeader("X-User-Id") Long userId, @RequestBody RazorpayOrderRequest request) throws Exception {
        log.info("Creating Razorpay order for user: {}, amount: {}", userId, request.getAmount());
        return ResponseEntity.ok(walletService.createRazorpayOrder(request.getAmount()));
    }

    // Verify a Razorpay payment signature and credit the wallet
    @PostMapping("/razorpay/verify")
    public ResponseEntity<Void> verifyPayment(@RequestHeader("X-User-Id") Long userId, @RequestBody PaymentVerifyRequest request) {
        log.info("Verifying payment for user: {}", userId);
        request.setUserId(userId);
        walletService.verifyPayment(request);
        return ResponseEntity.ok().build();
    }
}