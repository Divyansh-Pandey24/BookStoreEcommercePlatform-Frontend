package com.booknest.order.controller;

import com.booknest.order.dto.OrderResponse;
import com.booknest.order.dto.PlaceOrderRequest;
import com.booknest.order.exception.GlobalExceptionHandler;
import com.booknest.order.exception.ResourceNotFoundException;
import com.booknest.order.service.OrderServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderController Integration Tests (MockMvc)")
class OrderControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper = new ObjectMapper();

    @Mock private OrderServiceImpl orderService;

    @InjectMocks
    private OrderController orderController;

    private OrderResponse sampleOrder;

    @BeforeEach
    void setUp() {
        objectMapper.registerModule(new JavaTimeModule());
        mockMvc = MockMvcBuilders.standaloneSetup(orderController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        sampleOrder = new OrderResponse();
        sampleOrder.setOrderId(1L);
        sampleOrder.setUserId(10L);
        sampleOrder.setTotalAmount(998.0);
        sampleOrder.setOrderStatus("CONFIRMED");
        sampleOrder.setPlacedAt(LocalDateTime.now());
        sampleOrder.setItems(new ArrayList<>());
    }

    private PlaceOrderRequest validOrderRequest() {
        PlaceOrderRequest req = new PlaceOrderRequest();
        req.setPaymentMode("COD");
        req.setDeliveryName("John Doe");
        req.setDeliveryMobile("9876543210");
        req.setDeliveryAddress("123 Main St");
        req.setDeliveryCity("New York");
        req.setDeliveryPincode("10001");
        req.setDeliveryState("NY");
        return req;
    }

    @Test
    @DisplayName("POST /orders/place: success → 201 Created")
    void placeOrder_success() throws Exception {
        PlaceOrderRequest req = validOrderRequest();

        when(orderService.placeOrder(eq(10L), any(PlaceOrderRequest.class))).thenReturn(sampleOrder);

        mockMvc.perform(post("/orders/place")
                        .header("X-User-Id", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value(1L));
    }

    @Test
    @DisplayName("GET /orders/{orderId}: success → 200 OK")
    void getOrderById_success() throws Exception {
        when(orderService.getOrderById(1L, 10L, "CUSTOMER")).thenReturn(sampleOrder);

        mockMvc.perform(get("/orders/1")
                        .header("X-User-Id", 10L)
                        .header("X-User-Role", "CUSTOMER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(1L));
    }

    @Test
    @DisplayName("GET /orders/{orderId}: not found → 404 Not Found")
    void getOrderById_notFound_fails() throws Exception {
        when(orderService.getOrderById(99L, 10L, "CUSTOMER"))
                .thenThrow(new ResourceNotFoundException("Order not found with ID: 99"));

        mockMvc.perform(get("/orders/99")
                        .header("X-User-Id", 10L)
                        .header("X-User-Role", "CUSTOMER"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Order not found with ID: 99"));
    }

    @Test
    @DisplayName("GET /orders/my: success → 200 OK")
    void getMyOrders_success() throws Exception {
        when(orderService.getMyOrders(10L)).thenReturn(List.of(sampleOrder));

        mockMvc.perform(get("/orders/my")
                        .header("X-User-Id", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].orderId").value(1L));
    }

    @Test
    @DisplayName("DELETE /orders/{orderId}/cancel: success → 200 OK")
    void cancelOrder_success() throws Exception {
        when(orderService.cancelOrder(1L, 10L)).thenReturn(sampleOrder);

        mockMvc.perform(delete("/orders/1/cancel")
                        .header("X-User-Id", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(1L));
    }

    @Test
    @DisplayName("PATCH /orders/admin/{orderId}/status: success → 200 OK")
    void updateStatus_success() throws Exception {
        when(orderService.updateOrderStatus(eq(1L), eq("SHIPPED"), eq("ADMIN"))).thenReturn(sampleOrder);

        mockMvc.perform(patch("/orders/admin/1/status")
                        .header("X-User-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "SHIPPED"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(1L));
    }

    @Test
    @DisplayName("PATCH /orders/admin/{orderId}/status: unauthorized → 400 Bad Request")
    void updateStatus_unauthorized_fails() throws Exception {
        when(orderService.updateOrderStatus(eq(1L), eq("SHIPPED"), eq("CUSTOMER")))
                .thenThrow(new RuntimeException("Only admins can update order status"));

        mockMvc.perform(patch("/orders/admin/1/status")
                        .header("X-User-Role", "CUSTOMER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "SHIPPED"))))
                .andExpect(jsonPath("$.message").value("Only admins can update order status"));
    }

    @Test
    @DisplayName("GET /orders/admin/all: success → 200 OK")
    void getAllOrders_success() throws Exception {
        when(orderService.getAllOrders("ADMIN")).thenReturn(List.of(sampleOrder));

        mockMvc.perform(get("/orders/admin/all")
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].orderId").value(1L));
    }

    @Test
    @DisplayName("GET /orders/admin/status/{status}: success → 200 OK")
    void getByStatus_success() throws Exception {
        when(orderService.getOrdersByStatus("CONFIRMED", "ADMIN")).thenReturn(List.of(sampleOrder));

        mockMvc.perform(get("/orders/admin/status/CONFIRMED")
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].orderId").value(1L));
    }
}
