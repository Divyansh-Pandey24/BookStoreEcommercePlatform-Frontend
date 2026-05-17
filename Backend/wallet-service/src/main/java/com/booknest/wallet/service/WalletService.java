//package com.booknest.wallet.service;
//
//import com.booknest.wallet.dto.PaymentVerifyRequest;
//import com.booknest.wallet.dto.RazorpayOrderResponse;
//import com.booknest.wallet.dto.WalletDto;
//
//public interface WalletService {
//
//    WalletDto getWallet(Long userId);
//
//    // Called by order-service via Feign (with orderId)
//    void deductMoney(Long userId, Double amount, Long orderId);
//
//    // Called by order-service via Feign (with orderId)
//    void addMoney(Long userId, Double amount, Long orderId);
//
//    /*
//     * Called after Razorpay payment verification.
//     * No orderId needed for wallet top-up.
//     * Overloaded — different use case from addMoney above.
//     */
//    void addMoney(Long userId, Double amount);
//
//    RazorpayOrderResponse createRazorpayOrder(Double amount)
//            throws Exception;
//
//    void verifyPayment(PaymentVerifyRequest request);
//}

package com.booknest.wallet.service;

import com.booknest.wallet.dto.PaymentVerifyRequest;
import com.booknest.wallet.dto.RazorpayOrderResponse;
import com.booknest.wallet.dto.WalletDto;
import com.booknest.wallet.entity.Transaction;

import java.util.List;

public interface WalletService {

    WalletDto getWallet(Long userId);

    // FIX ✅: Added — required for GET /wallet/transactions endpoint
    List<Transaction> getTransactions(Long userId);

    // Called by order-service via Feign (with orderId)
    void deductMoney(Long userId, Double amount, Long orderId);

    // Called by order-service via Feign (with orderId)
    void addMoney(Long userId, Double amount, Long orderId);

    /*
     * Called after Razorpay payment verification.
     * No orderId needed for wallet top-up.
     * Overloaded — different use case from addMoney above.
     */
    void addMoney(Long userId, Double amount);

    RazorpayOrderResponse createRazorpayOrder(Double amount)
            throws Exception;

    void verifyPayment(PaymentVerifyRequest request);
}