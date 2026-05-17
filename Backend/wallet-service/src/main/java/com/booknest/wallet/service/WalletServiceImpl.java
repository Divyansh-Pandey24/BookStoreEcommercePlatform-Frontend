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

// Service implementation for managing user wallets, funds, and Razorpay integration
@Service
@RequiredArgsConstructor
@Slf4j
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final RazorpayService razorpayService;
    private final WalletEventProducer eventProducer;
    private final UserClient userClient;

    @Value("${razorpay.key.secret}")
    private String secret;

    // Retrieve a user's wallet, creating a new one if it doesn't already exist
    @Override
    public WalletDto getWallet(Long userId) {
        log.info("Retrieving wallet for user: {}", userId);
        Wallet wallet = walletRepository.findByUserId(userId).orElseGet(() -> createWallet(userId));
        return new WalletDto(wallet.getUserId(), wallet.getBalance());
    }

    // List all transactions for a user, sorted by the most recent first
    @Override
    public List<Transaction> getTransactions(Long userId) {
        log.info("Retrieving transactions for user: {}", userId);
        return transactionRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    // Deduct a specified amount from the user's wallet balance
    @Override
    @Transactional
    public void deductMoney(Long userId, Double amount, Long orderId) {
        Wallet wallet = walletRepository.findByUserId(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Wallet not found for user: " + userId));

        if (wallet.getBalance() < amount) {
            throw new RuntimeException("Insufficient balance. Required: ₹" + amount);
        }

        wallet.setBalance(wallet.getBalance() - amount);
        walletRepository.save(wallet);
        
        sendWalletEvent(userId, "PAYMENT_DEBIT", "₹" + amount + " deducted for order.");
        saveTransaction(userId, amount, "DEBIT", orderId);
    }

    // Add or refund a specified amount to the user's wallet balance
    @Override
    @Transactional
    public void addMoney(Long userId, Double amount, Long orderId) {
        Wallet wallet = walletRepository.findByUserId(userId).orElseGet(() -> createWallet(userId));
        wallet.setBalance(wallet.getBalance() + amount);
        walletRepository.save(wallet);

        String message = String.format("₹%.2f credited to your wallet. New Balance: ₹%.2f", amount, wallet.getBalance());
        sendWalletEvent(userId, "PAYMENT_CREDIT", message);
        saveTransaction(userId, amount, "CREDIT", orderId);
        log.info("Balance updated for user: {}, added: {}", userId, amount);
    }

    // Overload to add money without an associated order ID
    @Override
    @Transactional
    public void addMoney(Long userId, Double amount) {
        addMoney(userId, amount, null);
    }

    // Delegate Razorpay order creation to the external service
    @Override
    public RazorpayOrderResponse createRazorpayOrder(Double amount) throws Exception {
        return razorpayService.createOrder(amount);
    }

    // Verify the payment signature from Razorpay and credit the wallet accordingly
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void verifyPayment(PaymentVerifyRequest request) {
        log.info("Verifying Razorpay payment for user: {}", request.getUserId());
        try {
            JSONObject options = new JSONObject();
            options.put("razorpay_order_id", request.getRazorpayOrderId());
            options.put("razorpay_payment_id", request.getRazorpayPaymentId());
            options.put("razorpay_signature", request.getRazorpaySignature());

            // Verify signature
            Utils.verifyPaymentSignature(options, secret);

            if (request.getAmount() == null || request.getAmount() <= 0) {
                throw new RuntimeException("Invalid payment amount: " + request.getAmount());
            }

            // Only add money IF verification passes
            addMoney(request.getUserId(), request.getAmount());
            log.info("Payment verified and money added for user: {}", request.getUserId());
            
        } catch (Exception e) {
            log.error("Payment verification failed for user {}: {}", request.getUserId(), e.getMessage());
            // This throw triggers the Transactional rollback
            throw new RuntimeException("Payment verification failed. No money was added.");
        }
    }

    // Initialize a new wallet with zero balance for a user
    private Wallet createWallet(Long userId) {
        Wallet wallet = new Wallet();
        wallet.setUserId(userId);
        wallet.setBalance(0.0);
        wallet.setCreatedAt(LocalDateTime.now());
        wallet.setUpdatedAt(LocalDateTime.now());
        return walletRepository.save(wallet);
    }

    // Persist a transaction record to the history
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

    // Send an asynchronous wallet event notification ONLY after DB commit
    private void sendWalletEvent(Long userId, String type, String message) {
        // ✅ NEW: Use TransactionSynchronization to ensure message is only sent 
        // after the database transaction is successfully COMMITTED.
        if (org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()) {
            org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                new org.springframework.transaction.support.TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        executeSendEvent(userId, type, message);
                    }
                }
            );
        } else {
            executeSendEvent(userId, type, message);
        }
    }

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