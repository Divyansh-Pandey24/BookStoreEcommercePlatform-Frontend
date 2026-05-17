package com.booknest.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.booknest.order.client.BookClient;
import com.booknest.order.client.CartClient;
import com.booknest.order.client.UserClient;
import com.booknest.order.client.WalletClient;
import com.booknest.order.dto.BookDto;
import com.booknest.order.dto.CartDto;
import com.booknest.order.dto.CartItemDto;
import com.booknest.order.dto.OrderResponse;
import com.booknest.order.dto.PlaceOrderRequest;
import com.booknest.order.dto.UserProfileDto;
import com.booknest.order.dto.WalletDto;
import com.booknest.order.entity.Order;
import com.booknest.order.entity.OrderItem;
import com.booknest.order.event.OrderEventProducer;
import com.booknest.order.repository.OrderRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderServiceImpl Unit Tests")
class OrderServiceImplTest {

    @Mock private OrderRepository orderRepository;
    @Mock private CartClient cartClient;
    @Mock private BookClient bookClient;
    @Mock private WalletClient walletClient;
    @Mock private OrderEventProducer eventProducer;
    @Mock private UserClient userClient;

    @InjectMocks
    private OrderServiceImpl orderService;

    private CartDto cartWithOneItem;
    private CartItemDto cartItem;
    private BookDto availableBook;
    private WalletDto richWallet;
    private PlaceOrderRequest codRequest;
    private PlaceOrderRequest walletRequest;
    private Order savedOrder;

    @BeforeEach
    void setUp() {
        cartItem = new CartItemDto();
        cartItem.setBookId(1L);
        cartItem.setBookTitle("Clean Code");
        cartItem.setQuantity(2);
        cartItem.setPrice(499.0);
        cartItem.setSubtotal(998.0);

        cartWithOneItem = new CartDto();
        cartWithOneItem.setItems(List.of(cartItem));
        cartWithOneItem.setTotalPrice(998.0);

        availableBook = new BookDto();
        availableBook.setBookId(1L);
        availableBook.setStock(10);

        richWallet = new WalletDto();
        richWallet.setCurrentBalance(5000.0);

        codRequest = new PlaceOrderRequest();
        codRequest.setPaymentMode("COD");
        codRequest.setDeliveryName("Test User");
        codRequest.setDeliveryMobile("9876543210");
        codRequest.setDeliveryAddress("123 Main St");
        codRequest.setDeliveryCity("Mumbai");
        codRequest.setDeliveryPincode("400001");
        codRequest.setDeliveryState("Maharashtra");

        walletRequest = new PlaceOrderRequest();
        walletRequest.setPaymentMode("WALLET");
        walletRequest.setDeliveryName("Test User");
        walletRequest.setDeliveryMobile("9876543210");
        walletRequest.setDeliveryAddress("123 Main St");
        walletRequest.setDeliveryCity("Mumbai");
        walletRequest.setDeliveryPincode("400001");
        walletRequest.setDeliveryState("Maharashtra");

        OrderItem orderItem = new OrderItem();
        orderItem.setOrderItemId(1L);
        orderItem.setBookId(1L);
        orderItem.setBookTitle("Clean Code");
        orderItem.setQuantity(2);
        orderItem.setPrice(499.0);
        orderItem.setSubtotal(998.0);

        savedOrder = new Order();
        savedOrder.setOrderId(1L);
        savedOrder.setUserId(10L);
        savedOrder.setPaymentMode("COD");
        savedOrder.setTotalAmount(998.0);
        savedOrder.setOrderStatus("PLACED");
        savedOrder.setItems(new ArrayList<>(List.of(orderItem)));
        orderItem.setOrder(savedOrder);
    }

    // ─────────────────────────── PLACE ORDER ───────────────────────────

    @Test
    @DisplayName("placeOrder: COD, valid cart and stock → order saved, cart cleared, event published")
    void placeOrder_COD_success() {
        when(cartClient.getCart(10L)).thenReturn(cartWithOneItem);
        when(bookClient.getBookById(1L)).thenReturn(availableBook);
        when(bookClient.reserveStock(1L, 2)).thenReturn(true);
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        doNothing().when(cartClient).clearCart(10L);
        when(userClient.getUserProfile(10L)).thenReturn(new UserProfileDto());
        doNothing().when(eventProducer).sendOrderEvent(any());

        OrderResponse response = orderService.placeOrder(10L, codRequest);

        assertThat(response.getOrderStatus()).isEqualTo("PLACED");
        assertThat(response.getPaymentMode()).isEqualTo("COD");
        verify(bookClient).reserveStock(1L, 2);
        verify(orderRepository).save(any(Order.class));
        verify(cartClient).clearCart(10L);
    }

    @Test
    @DisplayName("placeOrder: WALLET payment, sufficient balance → deducted and order saved")
    void placeOrder_WALLET_success() {
        when(cartClient.getCart(10L)).thenReturn(cartWithOneItem);
        when(bookClient.getBookById(1L)).thenReturn(availableBook);
        when(walletClient.getWallet(10L)).thenReturn(richWallet);
        doNothing().when(walletClient).deductMoney(10L, 998.0);
        when(bookClient.reserveStock(1L, 2)).thenReturn(true);

        savedOrder.setPaymentMode("WALLET");
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        doNothing().when(cartClient).clearCart(10L);
        when(userClient.getUserProfile(10L)).thenReturn(new UserProfileDto());
        doNothing().when(eventProducer).sendOrderEvent(any());

        OrderResponse response = orderService.placeOrder(10L, walletRequest);

        assertThat(response).isNotNull();
        verify(walletClient).deductMoney(10L, 998.0);
    }

    @Test
    @DisplayName("placeOrder: empty cart → throws RuntimeException")
    void placeOrder_emptyCart_throws() {
        CartDto emptyCart = new CartDto();
        emptyCart.setItems(new ArrayList<>());
        when(cartClient.getCart(10L)).thenReturn(emptyCart);

        assertThatThrownBy(() -> orderService.placeOrder(10L, codRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Cart is empty");
    }

    @Test
    @DisplayName("placeOrder: invalid payment mode → throws RuntimeException")
    void placeOrder_invalidPaymentMode_throws() {
        codRequest.setPaymentMode("CREDIT_CARD");
        // Service validates payment mode before fetching cart — no cartClient stub needed

        assertThatThrownBy(() -> orderService.placeOrder(10L, codRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid payment mode");
    }

    @Test
    @DisplayName("placeOrder: insufficient stock → throws before wallet is touched")
    void placeOrder_insufficientStock_throws() {
        availableBook.setStock(1); // less than quantity 2
        when(cartClient.getCart(10L)).thenReturn(cartWithOneItem);
        when(bookClient.getBookById(1L)).thenReturn(availableBook);

        assertThatThrownBy(() -> orderService.placeOrder(10L, codRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Insufficient stock");

        verify(walletClient, never()).deductMoney(anyLong(), anyDouble());
    }

    @Test
    @DisplayName("placeOrder: insufficient wallet balance → throws RuntimeException")
    void placeOrder_insufficientWalletBalance_throws() {
        richWallet.setCurrentBalance(100.0); // less than 998.0
        when(cartClient.getCart(10L)).thenReturn(cartWithOneItem);
        when(bookClient.getBookById(1L)).thenReturn(availableBook);
        when(walletClient.getWallet(10L)).thenReturn(richWallet);

        assertThatThrownBy(() -> orderService.placeOrder(10L, walletRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Insufficient wallet balance");
    }

    @Test
    @DisplayName("placeOrder: stock reservation fails → compensates with release and refund")
    void placeOrder_stockReservationFails_compensates() {
        when(cartClient.getCart(10L)).thenReturn(cartWithOneItem);
        when(bookClient.getBookById(1L)).thenReturn(availableBook);
        when(walletClient.getWallet(10L)).thenReturn(richWallet);
        doNothing().when(walletClient).deductMoney(10L, 998.0);
        when(bookClient.reserveStock(1L, 2)).thenReturn(false);
        doNothing().when(walletClient).addMoney(10L, 998.0);

        assertThatThrownBy(() -> orderService.placeOrder(10L, walletRequest))
                .isInstanceOf(RuntimeException.class);

        verify(walletClient).addMoney(10L, 998.0); // refund issued
    }

    // ─────────────────────────── GET MY ORDERS ───────────────────────────

    @Test
    @DisplayName("getMyOrders: returns orders for specified user")
    void getMyOrders_returnsUserOrders() {
        when(orderRepository.findByUserIdOrderByPlacedAtDesc(10L))
                .thenReturn(List.of(savedOrder));

        List<OrderResponse> result = orderService.getMyOrders(10L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUserId()).isEqualTo(10L);
    }

    // ─────────────────────────── GET ORDER BY ID ───────────────────────────

    @Test
    @DisplayName("getOrderById: own order → success")
    void getOrderById_ownOrder_success() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(savedOrder));

        OrderResponse response = orderService.getOrderById(1L, 10L, "CUSTOMER");

        assertThat(response.getOrderId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getOrderById: another user's order → throws RuntimeException")
    void getOrderById_otherUserOrder_throws() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(savedOrder));

        assertThatThrownBy(() -> orderService.getOrderById(1L, 99L, "CUSTOMER"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Unauthorized access");
    }

    @Test
    @DisplayName("getOrderById: admin role can access any order")
    void getOrderById_admin_canAccessAnyOrder() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(savedOrder));

        OrderResponse response = orderService.getOrderById(1L, 99L, "ADMIN");

        assertThat(response).isNotNull();
    }

    // ─────────────────────────── GET ALL ORDERS ───────────────────────────

    @Test
    @DisplayName("getAllOrders: admin role → returns all orders")
    void getAllOrders_admin_success() {
        when(orderRepository.findAll()).thenReturn(List.of(savedOrder));

        List<OrderResponse> result = orderService.getAllOrders("ADMIN");

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("getOrdersByStatus: admin role → returns filtered orders")
    void getOrdersByStatus_admin_success() {
        savedOrder.setOrderStatus("CONFIRMED");
        when(orderRepository.findByOrderStatus("CONFIRMED")).thenReturn(List.of(savedOrder));

        List<OrderResponse> result = orderService.getOrdersByStatus("CONFIRMED", "ADMIN");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getOrderStatus()).isEqualTo("CONFIRMED");
    }

    @Test
    @DisplayName("getAllOrders: non-admin → throws RuntimeException")
    void getAllOrders_notAdmin_throws() {
        assertThatThrownBy(() -> orderService.getAllOrders("CUSTOMER"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Admin access required");
    }


    // ─────────────────────────── UPDATE ORDER STATUS ───────────────────────────

    @Test
    @DisplayName("updateOrderStatus: admin cancels WALLET order → refund and stock release")
    void updateOrderStatus_adminCancel_walletRefund() {
        savedOrder.setPaymentMode("WALLET");
        savedOrder.setOrderStatus("PLACED");
        when(orderRepository.findById(1L)).thenReturn(Optional.of(savedOrder));
        doNothing().when(walletClient).addMoney(10L, 998.0);
        doNothing().when(bookClient).releaseStock(anyLong(), anyInt());
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        when(userClient.getUserProfile(10L)).thenReturn(new UserProfileDto());
        doNothing().when(eventProducer).sendOrderEvent(any());

        orderService.updateOrderStatus(1L, "CANCELLED", "ADMIN");

        verify(walletClient).addMoney(10L, 998.0);
        verify(bookClient, atLeastOnce()).releaseStock(anyLong(), anyInt());
    }

    @Test
    @DisplayName("updateOrderStatus: admin cancels COD order → no refund issued")
    void updateOrderStatus_adminCancel_COD_noRefund() {
        savedOrder.setPaymentMode("COD");
        savedOrder.setOrderStatus("PLACED");
        when(orderRepository.findById(1L)).thenReturn(Optional.of(savedOrder));
        doNothing().when(bookClient).releaseStock(anyLong(), anyInt());
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        when(userClient.getUserProfile(10L)).thenReturn(new UserProfileDto());
        doNothing().when(eventProducer).sendOrderEvent(any());

        orderService.updateOrderStatus(1L, "CANCELLED", "ADMIN");

        verify(walletClient, never()).addMoney(anyLong(), anyDouble());
    }

    @Test
    @DisplayName("updateOrderStatus: invalid status → throws RuntimeException")
    void updateOrderStatus_invalidStatus_throws() {
        assertThatThrownBy(() -> orderService.updateOrderStatus(1L, "INVALID_STATE", "ADMIN"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid status update");
    }


    // ─────────────────────────── CANCEL ORDER ───────────────────────────

    @Test
    @DisplayName("cancelOrder: customer cancels own PLACED order → success")
    void cancelOrder_customer_success() {
        savedOrder.setOrderStatus("PLACED");
        savedOrder.setPaymentMode("COD");
        when(orderRepository.findById(1L)).thenReturn(Optional.of(savedOrder));
        doNothing().when(bookClient).releaseStock(anyLong(), anyInt());
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        when(userClient.getUserProfile(10L)).thenReturn(new UserProfileDto());
        doNothing().when(eventProducer).sendOrderEvent(any());

        OrderResponse response = orderService.cancelOrder(1L, 10L);

        assertThat(response).isNotNull();
        assertThat(savedOrder.getOrderStatus()).isEqualTo("CANCELLED");
    }

    @Test
    @DisplayName("cancelOrder: already cancelled → throws RuntimeException")
    void cancelOrder_alreadyCancelled_throws() {
        savedOrder.setOrderStatus("CANCELLED");
        when(orderRepository.findById(1L)).thenReturn(Optional.of(savedOrder));

        assertThatThrownBy(() -> orderService.cancelOrder(1L, 10L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("already cancelled");
    }

    @Test
    @DisplayName("cancelOrder: dispatched order → throws RuntimeException")
    void cancelOrder_dispatched_throws() {
        savedOrder.setOrderStatus("DISPATCHED");
        when(orderRepository.findById(1L)).thenReturn(Optional.of(savedOrder));

        assertThatThrownBy(() -> orderService.cancelOrder(1L, 10L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Cannot cancel order after dispatch");
    }


    @Test
    @DisplayName("cancelOrder: wrong user → throws RuntimeException")
    void cancelOrder_wrongUser_throws() {
        savedOrder.setOrderStatus("PLACED");
        when(orderRepository.findById(1L)).thenReturn(Optional.of(savedOrder));

        assertThatThrownBy(() -> orderService.cancelOrder(1L, 99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Unauthorized access");
    }

    @Test
    void placeOrder_cartFetchFailed_throws() {
        when(cartClient.getCart(10L)).thenThrow(new RuntimeException("Cart error"));
        assertThatThrownBy(() -> orderService.placeOrder(10L, codRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Could not fetch cart.");
    }

    @Test
    void placeOrder_bookFetchFailed_throws() {
        when(cartClient.getCart(10L)).thenReturn(cartWithOneItem);
        when(bookClient.getBookById(1L)).thenThrow(new RuntimeException("Book error"));
        assertThatThrownBy(() -> orderService.placeOrder(10L, codRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Could not verify stock");
    }

    @Test
    void placeOrder_walletFetchFailed_throws() {
        when(cartClient.getCart(10L)).thenReturn(cartWithOneItem);
        when(bookClient.getBookById(1L)).thenReturn(availableBook);
        when(walletClient.getWallet(10L)).thenThrow(new RuntimeException("Wallet error"));
        assertThatThrownBy(() -> orderService.placeOrder(10L, walletRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Wallet access failed.");
    }

    @Test
    void placeOrder_walletDeductionFailed_throws() {
        when(cartClient.getCart(10L)).thenReturn(cartWithOneItem);
        when(bookClient.getBookById(1L)).thenReturn(availableBook);
        when(walletClient.getWallet(10L)).thenReturn(richWallet);
        doThrow(new RuntimeException("Deduction error")).when(walletClient).deductMoney(anyLong(), anyDouble());
        assertThatThrownBy(() -> orderService.placeOrder(10L, walletRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Payment failed.");
    }

    @Test
    void placeOrder_saveOrderFailed_rollsBack() {
        when(cartClient.getCart(10L)).thenReturn(cartWithOneItem);
        when(bookClient.getBookById(1L)).thenReturn(availableBook);
        when(walletClient.getWallet(10L)).thenReturn(richWallet);
        doNothing().when(walletClient).deductMoney(anyLong(), anyDouble());
        when(bookClient.reserveStock(1L, 2)).thenReturn(true);
        when(orderRepository.save(any(Order.class))).thenThrow(new RuntimeException("DB error"));
        
        assertThatThrownBy(() -> orderService.placeOrder(10L, walletRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Finalizing order failed.");
        
        verify(bookClient).releaseStock(1L, 2);
        verify(walletClient).addMoney(10L, 998.0);
    }

    @Test
    void updateOrderStatus_deliveredCannotBeModified() {
        savedOrder.setOrderStatus("DELIVERED");
        when(orderRepository.findById(1L)).thenReturn(Optional.of(savedOrder));
        assertThatThrownBy(() -> orderService.updateOrderStatus(1L, "CANCELLED", "ADMIN"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Delivered orders cannot be modified");
    }

    @Test
    void updateOrderStatus_cancelWalletOrderRefundFails_throws() {
        savedOrder.setPaymentMode("WALLET");
        savedOrder.setOrderStatus("PLACED");
        when(orderRepository.findById(1L)).thenReturn(Optional.of(savedOrder));
        doThrow(new RuntimeException("Refund API error")).when(walletClient).addMoney(anyLong(), anyDouble());
        
        assertThatThrownBy(() -> orderService.updateOrderStatus(1L, "CANCELLED", "ADMIN"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Refund failed.");
    }

    @Test
    void cancelOrder_walletRefundFails_throws() {
        savedOrder.setPaymentMode("WALLET");
        savedOrder.setOrderStatus("PLACED");
        when(orderRepository.findById(1L)).thenReturn(Optional.of(savedOrder));
        doThrow(new RuntimeException("Refund API error")).when(walletClient).addMoney(anyLong(), anyDouble());
        
        assertThatThrownBy(() -> orderService.cancelOrder(1L, 10L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Refund failed.");
    }
}
