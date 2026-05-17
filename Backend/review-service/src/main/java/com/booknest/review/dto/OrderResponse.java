package com.booknest.review.dto;

import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class OrderResponse {
    private Long              orderId;
    private String            orderStatus;
    private List<OrderItemDto> items;
}