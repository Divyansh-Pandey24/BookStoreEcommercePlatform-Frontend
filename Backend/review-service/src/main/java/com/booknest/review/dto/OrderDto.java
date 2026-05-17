package com.booknest.review.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
public class OrderDto {
    private Long         orderId;
    private Long         userId;
    private String       orderStatus;
    /*
     * List of bookIds in this order.
     * We need this to check if customer bought a specific book.
     * Order Service returns OrderResponse which has items list.
     * We extract bookIds from items in the controller/service.
     *
     * Actually simpler: Order Service returns OrderResponse
     * which has List<OrderItemResponse> items.
     * Each item has a bookId.
     * We map them here for easy checking.
     */
    private List<Long>   bookIds;
}