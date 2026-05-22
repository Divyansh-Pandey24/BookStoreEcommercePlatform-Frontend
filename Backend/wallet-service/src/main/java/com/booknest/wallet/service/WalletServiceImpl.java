package com.booknest.wallet.service;

import java.time.LocalDateTime;
import java.util.List;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.booknest.wallet.client.UserClient;
import com.booknest.wallet.dto.*;
import com.booknest.wallet.entity.Transaction;
import com.booknest.wallet.entity.Wallet;
import com.booknest.wallet.exception.ResourceNotFoundException;
import com.booknest.wallet.event.WalletEventProducer;
import com.booknest.wallet.external.RazorpayService;
import com.booknest.wallet.repository.TransactionRepository;
import com.booknest.wallet.repository.WalletRepository;
import com.razorpay.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// This service implements the business logic for managing user digital
@Service
@RequiredArgsConstructor
@Slf4j
public class WalletServiceImpl implements WalletService {

    /**
     * Repository used to perform SQL CRUD actions on digital Wallet entities.
     */
    private final WalletRepository walletRepository;

    /**
     * Repository used to perform SQL CRUD actions on Transaction logs.
     */
    private final TransactionRepository transactionRepository;

    /**
     * External service managing Razorpay REST client order creations.
     */
    private final RazorpayService razorpayService;

    /**
     * Kafka event producer used to publish real-time wallet notification alerts.
     */
    private final WalletEventProducer eventProducer;

    /**
     * Feign client used to retrieve customer contact profiles (email, mobile) for
     * notifications.
     */
    private final UserClient userClient;

    /**
     * Razorpay credential secret key used to compute and verify webhook signature
     * authenticity.
     */
    @Value("${razorpay.key.secret}")
    private String secret;

    // Retrieves a user's wallet, dynamically creating a new empty wallet
    @Override
    public WalletDto getWallet(Long userId) {
        log.info("Retrieving wallet for user: {}", userId);
        Wallet wallet = walletRepository.findByUserId(userId).orElseGet(() -> createWallet(userId));
        return new WalletDto(wallet.getUserId(), wallet.getBalance());
    }

    // Lists all transaction logs for a user, sorted from most recent to
    @Override
    public List<Transaction> getTransactions(Long userId) {
        log.info("Retrieving transactions for user: {}", userId);
        return transactionRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    // Deducts a specified amount from the user's wallet balance.
    @Override
    @Transactional
    public void deductMoney(Long userId, Double amount, Long orderId) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found for user: " + userId));

        if (wallet.getBalance() < amount) {
            throw new RuntimeException("Insufficient balance. Required: ₹" + amount);
        }

        // Subtract cost from user's balance.
        wallet.setBalance(wallet.getBalance() - amount);
        walletRepository.save(wallet);

        // Dispatch notifications and record the success in the transaction table.
        sendWalletEvent(userId, "PAYMENT_DEBIT", "₹" + amount + " deducted for order.");
        saveTransaction(userId, amount, "DEBIT", orderId);
    }

    // Credits a specified amount to the user's wallet balance (e.g. for
    @Override
    @Transactional
    public void addMoney(Long userId, Double amount, Long orderId) {
        Wallet wallet = walletRepository.findByUserId(userId).orElseGet(() -> createWallet(userId));
        wallet.setBalance(wallet.getBalance() + amount);
        walletRepository.save(wallet);

        String message = String.format("₹%.2f credited to your wallet. New Balance: ₹%.2f", amount,
                wallet.getBalance());
        sendWalletEvent(userId, "PAYMENT_CREDIT", message);
        saveTransaction(userId, amount, "CREDIT", orderId);
        log.info("Balance updated for user: {}, added: {}", userId, amount);
    }

    // Overload helper to credit funds without an associated order ID.
    @Override
    @Transactional
    public void addMoney(Long userId, Double amount) {
        addMoney(userId, amount, null);
    }

    // Assembles a Razorpay billing order token.
    @Override
    public RazorpayOrderResponse createRazorpayOrder(Double amount) throws Exception {
        return razorpayService.createOrder(amount);
    }

    // Verifies authenticity of Razorpay payment signatures and credits
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void verifyPayment(PaymentVerifyRequest request) {
        log.info("Verifying Razorpay payment for user: {}", request.getUserId());
        try {
            JSONObject options = new JSONObject();
            options.put("razorpay_order_id", request.getRazorpayOrderId());
            options.put("razorpay_payment_id", request.getRazorpayPaymentId());
            options.put("razorpay_signature", request.getRazorpaySignature());

            // Validate signature against the local secret using Razorpay's HMAC validator.
            Utils.verifyPaymentSignature(options, secret);

            if (request.getAmount() == null || request.getAmount() <= 0) {
                throw new RuntimeException("Invalid payment amount: " + request.getAmount());
            }

            // Signature check passed. Credit the user's wallet balance.
            addMoney(request.getUserId(), request.getAmount());
            log.info("Payment verified and money added for user: {}", request.getUserId());

        } catch (Exception e) {
            log.error("Payment verification failed for user {}: {}", request.getUserId(), e.getMessage());
            // Throw exception to trigger a rollback, ensuring no funds are credited on
            // invalid signatures.
            throw new RuntimeException("Payment verification failed. No money was added.");
        }
    }

    // Generates and saves a new Wallet database record with a zero
    private Wallet createWallet(Long userId) {
        Wallet wallet = new Wallet();
        wallet.setUserId(userId);
        wallet.setBalance(0.0);
        wallet.setCreatedAt(LocalDateTime.now());
        wallet.setUpdatedAt(LocalDateTime.now());
        return walletRepository.save(wallet);
    }

    // Saves a transaction log record to the database ledger.
    private void saveTransaction(Long userId, Double amount, String type, Long orderId) {
        Transaction txn = new Transaction();
        txn.setUserId(userId);
        txn.setAmount(amount);
        txn.setType(type);
        txn.setStatus("SUCCESS");
        txn.setOrderId(orderId);
        txn.setCreatedAt(LocalDateTime.now());
        transactionRepository.save(txn);
    }

    // Schedules a wallet alert event to be published via Kafka.
    private void sendWalletEvent(Long userId, String type, String message) {
        // Register a transaction synchronization hook if a database transaction is
        // active.
        if (org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()) {
            org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                    new org.springframework.transaction.support.TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            executeSendEvent(userId, type, message);
                        }
                    });
        } else {
            // Dispatch immediately if no transaction context exists.
            executeSendEvent(userId, type, message);
        }
    }

    // Fetches customer contact details (email, mobile) and publishes the
    private void executeSendEvent(Long userId, String type, String message) {
        String email = "";
        String mobile = "";

        try {
            UserProfileDto user = userClient.getUserProfile(userId);
            if (user != null) {
                email = user.getEmail() != null ? user.getEmail() : "";
                mobile = user.getMobile() != null ? user.getMobile() : "";
            }
        } catch (Exception e) {
            log.warn("User details fetch failed for notification: {}", e.getMessage());
        }

        eventProducer.sendWalletEvent(new WalletEventDto(userId, type, message, email, mobile));
        log.info("Wallet event published: type={}, userId={}", type, userId);
    }

}