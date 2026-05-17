package com.booknest.cart.controller;

import com.booknest.cart.dto.AddToCartRequest;
import com.booknest.cart.dto.CartResponse;
import com.booknest.cart.exception.GlobalExceptionHandler;
import com.booknest.cart.exception.ResourceNotFoundException;
import com.booknest.cart.service.CartServiceImpl;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CartController Integration Tests (MockMvc)")
class CartControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper = new ObjectMapper();

    @Mock private CartServiceImpl cartService;

    @InjectMocks
    private CartController cartController;

    private CartResponse sampleCart;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(cartController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        sampleCart = new CartResponse();
        sampleCart.setCartId(1L);
        sampleCart.setUserId(10L);
        sampleCart.setTotalPrice(998.0);
        sampleCart.setItems(new ArrayList<>());
    }

    @Test
    @DisplayName("GET /cart: success → 200 OK")
    void getCart_success() throws Exception {
        when(cartService.getCartByUser(10L)).thenReturn(sampleCart);

        mockMvc.perform(get("/cart")
                        .header("X-User-Id", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(10L));
    }

    @Test
    @DisplayName("POST /cart/add: success → 200 OK")
    void addItem_success() throws Exception {
        AddToCartRequest req = new AddToCartRequest();
        req.setBookId(100L);
        req.setQuantity(1);

        when(cartService.addItem(eq(10L), any(AddToCartRequest.class))).thenReturn(sampleCart);

        mockMvc.perform(post("/cart/add")
                        .header("X-User-Id", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cartId").value(1L));
    }

    @Test
    @DisplayName("POST /cart/add: book not found → 404 Not Found")
    void addItem_notFound_fails() throws Exception {
        AddToCartRequest req = new AddToCartRequest();
        req.setBookId(999L);
        req.setQuantity(1);

        when(cartService.addItem(eq(10L), any(AddToCartRequest.class)))
                .thenThrow(new ResourceNotFoundException("Book not found"));

        mockMvc.perform(post("/cart/add")
                        .header("X-User-Id", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Book not found"));
    }

    @Test
    @DisplayName("DELETE /cart/clear: success → 200 OK")
    void clearCart_success() throws Exception {
        doNothing().when(cartService).clearCart(10L);

        mockMvc.perform(delete("/cart/clear")
                        .header("X-User-Id", 10L))
                .andExpect(status().isOk())
                .andExpect(content().string("Cart cleared."));
    }

    @Test
    @DisplayName("PATCH /cart/item/{itemId}: success → 200 OK")
    void updateQuantity_success() throws Exception {
        when(cartService.updateQuantity(eq(10L), eq(1L), eq(5))).thenReturn(sampleCart);

        mockMvc.perform(patch("/cart/item/1")
                        .header("X-User-Id", 10L)
                        .param("quantity", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cartId").value(1L));
    }

    @Test
    @DisplayName("DELETE /cart/item/{itemId}: success → 200 OK")
    void removeItem_success() throws Exception {
        when(cartService.removeItem(10L, 1L)).thenReturn(sampleCart);

        mockMvc.perform(delete("/cart/item/1")
                        .header("X-User-Id", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cartId").value(1L));
    }

    @Test
    @DisplayName("GET /cart/count: success → 200 OK")
    void getCount_success() throws Exception {
        when(cartService.getItemCount(10L)).thenReturn(5);

        mockMvc.perform(get("/cart/count")
                        .header("X-User-Id", 10L))
                .andExpect(status().isOk())
                .andExpect(content().string("5"));
    }
}
