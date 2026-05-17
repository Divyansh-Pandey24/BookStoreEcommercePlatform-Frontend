package com.booknest.order.service;

import com.booknest.order.client.BookClient;
import com.booknest.order.client.CartClient;
import com.booknest.order.client.UserClient;
import com.booknest.order.client.WalletClient;
import com.booknest.order.dto.*;
import com.booknest.order.entity.Order;
import com.booknest.order.entity.OrderItem;
import com.booknest.order.event.OrderEventProducer;
import com.booknest.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private CartClient cartClient;
    @Mock
    private BookClient bookClient;
    @Mock
    private WalletClient walletClient;
    @Mock
    private OrderEventProducer eventProducer;
    @Mock
    private UserClient userClient;

    @InjectMocks
    private OrderServiceImpl orderService;

    private Order sampleOrder;

    @BeforeEach
    void setUp() {
        sampleOrder = new Order();
        sampleOrder.setOrderId(1L);
        sampleOrder.setUserId(1L);
        sampleOrder.setTotalAmount(500.0);
        sampleOrder.setOrderStatus("PLACED");
        sampleOrder.setPaymentMode("COD");
        sampleOrder.setItems(new ArrayList<>());
    }

    @Test
    void testGetMyOrders() {
        when(orderRepository.findByUserIdOrderByPlacedAtDesc(1L)).thenReturn(List.of(sampleOrder));
        List<OrderResponse> orders = orderService.getMyOrders(1L);
        assertEquals(1, orders.size());
        assertEquals(1L, orders.get(0).getOrderId());
    }

    @Test
    void testGetOrderById_Success() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(sampleOrder));
        OrderResponse response = orderService.getOrderById(1L, 1L, "CUSTOMER");
        assertNotNull(response);
        assertEquals(1L, response.getOrderId());
    }

    @Test
    void testGetOrderById_Unauthorized() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(sampleOrder));
        assertThrows(RuntimeException.class, () -> orderService.getOrderById(1L, 2L, "CUSTOMER"));
    }

    @Test
    void testUpdateOrderStatus_Admin() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(sampleOrder));
        when(orderRepository.save(any())).thenReturn(sampleOrder);
        
        OrderResponse response = orderService.updateOrderStatus(1L, "CONFIRMED", "ADMIN");
        
        assertEquals("CONFIRMED", response.getOrderStatus());
        verify(orderRepository, times(1)).save(any());
    }

    @Test
    void testCancelOrder_Success() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(sampleOrder));
        when(orderRepository.save(any())).thenReturn(sampleOrder);
        
        OrderResponse response = orderService.cancelOrder(1L, 1L);
        
        assertEquals("CANCELLED", response.getOrderStatus());
        verify(orderRepository, times(1)).save(any());
    }

    @Test
    void testCancelOrder_AfterDispatch() {
        sampleOrder.setOrderStatus("DISPATCHED");
        when(orderRepository.findById(1L)).thenReturn(Optional.of(sampleOrder));
        assertThrows(RuntimeException.class, () -> orderService.cancelOrder(1L, 1L));
    }
}
