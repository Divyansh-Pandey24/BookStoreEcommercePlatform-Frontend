package com.booknest.order.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

/*
 * What we send back to React after placing or viewing an order.
 */
@Data
@NoArgsConstructor
public class OrderResponse {
    private Long   orderId;
    private Long   userId;
    private String paymentMode;
    private Double totalAmount;
    private String orderStatus;

    // Delivery address
    private String deliveryName;
    private String deliveryMobile;
    private String deliveryAddress;
    private String deliveryCity;
    private String deliveryPincode;
    private String deliveryState;

    private LocalDateTime placedAt;
    private LocalDateTime updatedAt;

    // All books in this order
    private List<OrderItemResponse> items;
}