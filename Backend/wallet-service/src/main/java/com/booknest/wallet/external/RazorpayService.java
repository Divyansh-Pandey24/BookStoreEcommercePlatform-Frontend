package com.booknest.wallet.external;

import com.booknest.wallet.dto.RazorpayOrderResponse;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

// Service for interacting with the external Razorpay payment gateway
@Slf4j
@Service
public class RazorpayService {

    private final RazorpayClient razorpayClient;

    @Value("${razorpay.key.id}")
    private String keyId;

    public RazorpayService(RazorpayClient razorpayClient) {
        this.razorpayClient = razorpayClient;
    }

    // Create a new payment order in Razorpay (amount converted to paise)
    public RazorpayOrderResponse createOrder(Double amount) throws Exception {
        log.info("Initiating Razorpay order for amount: {}", amount);

        JSONObject options = new JSONObject();
        options.put("amount", (int) Math.round(amount * 100));
        options.put("currency", "INR");

        Order order = razorpayClient.orders.create(options);
        return new RazorpayOrderResponse(order.get("id"), keyId, amount, "INR");
    }
}