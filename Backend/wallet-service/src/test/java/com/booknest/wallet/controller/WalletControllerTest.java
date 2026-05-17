package com.booknest.wallet.controller;

import com.booknest.wallet.dto.PaymentVerifyRequest;
import com.booknest.wallet.dto.RazorpayOrderRequest;
import com.booknest.wallet.dto.RazorpayOrderResponse;
import com.booknest.wallet.dto.WalletDto;
import com.booknest.wallet.exception.GlobalExceptionHandler;
import com.booknest.wallet.service.WalletService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.ArrayList;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WalletController Integration Tests (MockMvc)")
class WalletControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper = new ObjectMapper();

    @Mock private WalletService walletService;

    @InjectMocks
    private WalletController walletController;

    private WalletDto sampleWallet;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(walletController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        
        // Inject the secret key into the controller so it doesn't fail with NULL
        org.springframework.test.util.ReflectionTestUtils.setField(walletController, "gatewaySecret", "test-secret");
        
        sampleWallet = new WalletDto(10L, 500.0);
    }

    private static final String GATEWAY_SECRET = "test-secret";

    @Test
    @DisplayName("GET /wallet: success → 200 OK")
    void getMyWallet_success() throws Exception {
        when(walletService.getWallet(10L)).thenReturn(sampleWallet);

        mockMvc.perform(get("/wallet")
                        .header("X-User-Id", 10L))
                .andExpect(jsonPath("$.currentBalance").value(500.0));
    }

    @Test
    @DisplayName("GET /wallet/{userId}: success → 200 OK")
    void getWallet_userId_success() throws Exception {
        when(walletService.getWallet(10L)).thenReturn(sampleWallet);

        mockMvc.perform(get("/wallet/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentBalance").value(500.0));
    }

    @Test
    @DisplayName("GET /wallet/transactions: success → 200 OK")
    void getMyTransactions_success() throws Exception {
        when(walletService.getTransactions(10L)).thenReturn(new ArrayList<>());

        mockMvc.perform(get("/wallet/transactions")
                        .header("X-User-Id", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("POST /wallet/{userId}/deduct: success → 200 OK")
    void deductMoney_success() throws Exception {
        doNothing().when(walletService).deductMoney(eq(10L), eq(50.0), isNull());

        mockMvc.perform(post("/wallet/10/deduct")
                        .header("X-Gateway-Secret", GATEWAY_SECRET)
                        .param("amount", "50.0"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /wallet/{userId}/deduct: insufficient funds → 400 Bad Request")
    void deductMoney_insufficientFunds_fails() throws Exception {
        doThrow(new RuntimeException("Insufficient balance in wallet"))
                .when(walletService).deductMoney(eq(10L), eq(1000.0), isNull());

        mockMvc.perform(post("/wallet/10/deduct")
                        .header("X-Gateway-Secret", GATEWAY_SECRET)
                        .param("amount", "1000.0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Insufficient balance in wallet"));
    }

    @Test
    @DisplayName("POST /wallet/{userId}/deduct: unauthorized → 403 Forbidden")
    void deductMoney_unauthorized_fails() throws Exception {
        mockMvc.perform(post("/wallet/10/deduct")
                        .header("X-Gateway-Secret", "wrong-secret")
                        .param("amount", "50.0"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /wallet/{userId}/add: success → 200 OK")
    void addMoney_success() throws Exception {
        doNothing().when(walletService).addMoney(eq(10L), eq(100.0), isNull());

        mockMvc.perform(post("/wallet/10/add")
                        .header("X-Gateway-Secret", GATEWAY_SECRET)
                        .param("amount", "100.0"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /wallet/{userId}/add: unauthorized → 403 Forbidden")
    void addMoney_unauthorized_fails() throws Exception {
        mockMvc.perform(post("/wallet/10/add")
                        .header("X-Gateway-Secret", "wrong-secret")
                        .param("amount", "100.0"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /wallet/razorpay/create-order: success → 200 OK")
    void createOrder_success() throws Exception {
        RazorpayOrderResponse rResponse = new RazorpayOrderResponse("rzp_123", "key_456", 200.0, "INR");
        RazorpayOrderRequest rRequest = new RazorpayOrderRequest();
        rRequest.setAmount(200.0);

        when(walletService.createRazorpayOrder(200.0)).thenReturn(rResponse);

        mockMvc.perform(post("/wallet/razorpay/create-order")
                        .header("X-User-Id", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.razorpayOrderId").value("rzp_123"));
    }

    @Test
    @DisplayName("POST /wallet/razorpay/verify: success → 200 OK")
    void verifyPayment_success() throws Exception {
        PaymentVerifyRequest pRequest = new PaymentVerifyRequest();
        pRequest.setRazorpayOrderId("rzp_123");
        pRequest.setRazorpayPaymentId("pay_456");
        pRequest.setRazorpaySignature("sig_789");

        doNothing().when(walletService).verifyPayment(any(PaymentVerifyRequest.class));

        mockMvc.perform(post("/wallet/razorpay/verify")
                        .header("X-User-Id", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pRequest)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /wallet/razorpay/verify: invalid signature → 400 Bad Request")
    void verifyPayment_invalidSignature_fails() throws Exception {
        PaymentVerifyRequest pRequest = new PaymentVerifyRequest();
        pRequest.setRazorpayOrderId("rzp_123");
        
        doThrow(new RuntimeException("Invalid payment signature"))
                .when(walletService).verifyPayment(any(PaymentVerifyRequest.class));

        mockMvc.perform(post("/wallet/razorpay/verify")
                        .header("X-User-Id", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid payment signature"));
    }
}
