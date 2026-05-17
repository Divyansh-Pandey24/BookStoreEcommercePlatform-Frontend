package com.booknest.cart.service;

import com.booknest.cart.client.BookClient;
import com.booknest.cart.dto.AddToCartRequest;
import com.booknest.cart.dto.BookResponse;
import com.booknest.cart.dto.CartResponse;
import com.booknest.cart.entity.Cart;
import com.booknest.cart.entity.CartItem;
import com.booknest.cart.repository.CartItemRepository;
import com.booknest.cart.repository.CartRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CartServiceTest {

    @Mock
    private CartRepository cartRepository;
    @Mock
    private CartItemRepository cartItemRepository;
    @Mock
    private BookClient bookClient;

    @InjectMocks
    private CartServiceImpl cartService;

    private Cart sampleCart;

    @BeforeEach
    void setUp() {
        sampleCart = new Cart();
        sampleCart.setCartId(1L);
        sampleCart.setUserId(1L);
        sampleCart.setTotalPrice(0.0);
        sampleCart.setItems(new ArrayList<>());
    }

    @Test
    void testGetCartByUser_Existing() {
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(sampleCart));
        CartResponse response = cartService.getCartByUser(1L);
        assertNotNull(response);
        assertEquals(1L, response.getCartId());
    }

    @Test
    void testGetCartByUser_New() {
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(cartRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);
        
        CartResponse response = cartService.getCartByUser(1L);
        assertNotNull(response);
        verify(cartRepository, times(1)).save(any());
    }

    @Test
    void testAddItem_Success() {
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(sampleCart));
        
        BookResponse book = new BookResponse();
        book.setBookId(101L);
        book.setTitle("Test Book");
        book.setPrice(200.0);
        book.setStock(10);
        book.setActive(true);
        
        when(bookClient.getBookById(101L)).thenReturn(book);
        when(cartRepository.save(any())).thenReturn(sampleCart);

        AddToCartRequest request = new AddToCartRequest();
        request.setBookId(101L);
        request.setQuantity(2);

        CartResponse response = cartService.addItem(1L, request);
        
        assertNotNull(response);
        assertEquals(400.0, response.getTotalPrice());
        assertEquals(1, response.getTotalItems());
    }

    @Test
    void testAddItem_OutOfStock() {
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(sampleCart));
        
        BookResponse book = new BookResponse();
        book.setStock(0);
        book.setActive(true);
        
        when(bookClient.getBookById(101L)).thenReturn(book);

        AddToCartRequest request = new AddToCartRequest();
        request.setBookId(101L);
        request.setQuantity(1);

        assertThrows(RuntimeException.class, () -> cartService.addItem(1L, request));
    }

    @Test
    void testClearCart() {
        sampleCart.getItems().add(new CartItem());
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(sampleCart));
        
        cartService.clearCart(1L);
        
        assertTrue(sampleCart.getItems().isEmpty());
        assertEquals(0.0, sampleCart.getTotalPrice());
        verify(cartRepository, times(1)).save(sampleCart);
    }
}
