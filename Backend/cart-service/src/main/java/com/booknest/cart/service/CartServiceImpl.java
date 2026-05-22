package com.booknest.cart.service;

import com.booknest.cart.client.BookClient;
import com.booknest.cart.dto.*;
import com.booknest.cart.entity.Cart;
import com.booknest.cart.entity.CartItem;
import com.booknest.cart.exception.ResourceNotFoundException;
import com.booknest.cart.repository.CartItemRepository;
import com.booknest.cart.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.transaction.annotation.Transactional;

// This service implements the central business logic for managing user shopping carts in BookNest.
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CartServiceImpl {

    /**
     * Repository used to perform SQL operations on the root Cart entities.
     */
    private final CartRepository cartRepository;

    /**
     * Repository used to perform SQL operations on individual CartItem entries.
     */
    private final CartItemRepository cartItemRepository;

    /**
     * Feign client used to retrieve catalog information from the Book microservice.
     */
    private final BookClient bookClient;

    // Helper method to map a Cart entity to a CartResponse data transfer object (DTO).
    private CartResponse toResponse(Cart cart) {
        List<CartResponse.CartItemResponse> itemResponses = new ArrayList<>();
        for (CartItem item : cart.getItems()) {
            CartResponse.CartItemResponse r = new CartResponse.CartItemResponse();
            r.setItemId(item.getItemId());
            r.setBookId(item.getBookId());
            r.setBookTitle(item.getBookTitle());
            r.setCoverImageUrl(item.getCoverImageUrl());
            r.setPrice(item.getPrice());
            r.setQuantity(item.getQuantity());
            r.setSubtotal(item.getPrice() * item.getQuantity());
            itemResponses.add(r);
        }

        CartResponse response = new CartResponse();
        response.setCartId(cart.getCartId());
        response.setUserId(cart.getUserId());
        response.setTotalPrice(cart.getTotalPrice());
        response.setTotalItems(cart.getItems().size());
        response.setUpdatedAt(cart.getUpdatedAt());
        response.setItems(itemResponses);
        return response;
    }

    // Recalculates the total price of a cart based on its items and their quantities.
    private void recalculateTotal(Cart cart) {
        double total = 0.0;
        for (CartItem item : cart.getItems()) {
            total += item.getPrice() * item.getQuantity();
        }
        cart.setTotalPrice(total);
    }

    // Safely retrieves book metadata from the Book Service via Feign with retry/error handling.
    private BookResponse getBook(Long bookId) {
        try {
            BookResponse book = bookClient.getBookById(bookId);
            if (book == null) throw new ResourceNotFoundException("Book not found. id=" + bookId);
            return book;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Feign call to Book Service failed: {}", e.getMessage());
            throw new RuntimeException("Could not retrieve book details.");
        }
    }

    // Fetches the shopping cart for a specific user.
    public CartResponse getCartByUser(Long userId) {
        log.info("Fetching cart for user: {}", userId);
        Cart cart = cartRepository.findByUserId(userId).orElseGet(() -> {
            Cart newCart = new Cart();
            newCart.setUserId(userId);
            return cartRepository.save(newCart);
        });
        return toResponse(cart);
    }

    // Adds a book to a user's shopping cart.
    public CartResponse addItem(Long userId, AddToCartRequest request) {
        Cart cart = cartRepository.findByUserId(userId).orElseGet(() -> {
            Cart newCart = new Cart();
            newCart.setUserId(userId);
            return cartRepository.save(newCart);
        });

        // Step 1: Validate catalog details and check real-time stock levels.
        BookResponse book = getBook(request.getBookId());
        if (Boolean.FALSE.equals(book.getActive())) throw new RuntimeException("Book is no longer available.");
        if (book.getStock() == null || book.getStock() <= 0) throw new RuntimeException("Book is out of stock.");
        if (request.getQuantity() > book.getStock()) throw new RuntimeException("Requested quantity exceeds available stock.");

        // Step 2: Check if the book is already in the cart.
        Optional<CartItem> existingItem = cart.getItems().stream()
                .filter(i -> i.getBookId().equals(request.getBookId())).findFirst();

        if (existingItem.isPresent()) {
            // Step 3: Increment quantity if item exists, ensuring it does not exceed total available catalog stock.
            CartItem item = existingItem.get();
            int newQty = item.getQuantity() + request.getQuantity();
            if (newQty > book.getStock()) throw new RuntimeException("Total quantity exceeds available stock.");
            item.setQuantity(newQty);
        } else {
            // Step 4: Create a new CartItem record and associate it with the parent Cart.
            CartItem newItem = new CartItem();
            newItem.setBookId(request.getBookId());
            newItem.setBookTitle(book.getTitle());
            newItem.setPrice(book.getPrice());
            newItem.setCoverImageUrl(book.getCoverImageUrl());
            newItem.setQuantity(request.getQuantity());
            newItem.setCart(cart);
            cart.getItems().add(newItem);
        }

        // Step 5: Recalculate cart totals, update timestamp, and save changes.
        recalculateTotal(cart);
        cart.setUpdatedAt(LocalDateTime.now());
        return toResponse(cartRepository.save(cart));
    }

    // Removes a specific item from a user's shopping cart.
    public CartResponse removeItem(Long userId, Long itemId) {
        Cart cart = cartRepository.findByUserId(userId).orElseThrow(() -> new RuntimeException("Cart not found."));
        
        // Remove item from the cart collection.
        boolean removed = cart.getItems().removeIf(item -> item.getItemId().equals(itemId));
        if (!removed) throw new RuntimeException("Item not found in cart.");
        
        recalculateTotal(cart);
        cart.setUpdatedAt(LocalDateTime.now());
        return toResponse(cartRepository.save(cart));
    }

    // Explicitly updates the quantity of an item in a user's cart.
    public CartResponse updateQuantity(Long userId, Long itemId, Integer quantity) {
        if (quantity < 1) throw new RuntimeException("Quantity must be at least 1.");
        Cart cart = cartRepository.findByUserId(userId).orElseThrow(() -> new RuntimeException("Cart not found."));
        
        CartItem item = cart.getItems().stream().filter(i -> i.getItemId().equals(itemId)).findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Item not found in cart."));

        item.setQuantity(quantity);
        recalculateTotal(cart);
        cart.setUpdatedAt(LocalDateTime.now());
        return toResponse(cartRepository.save(cart));
    }

    // Empties all items from a user's shopping cart.
    public void clearCart(Long userId) {
        Cart cart = cartRepository.findByUserId(userId).orElseThrow(() -> new ResourceNotFoundException("Cart not found."));
        cart.getItems().clear();
        cart.setTotalPrice(0.0);
        cart.setUpdatedAt(LocalDateTime.now());
        cartRepository.save(cart);
    }

    // Fetches the total number of items in a user's cart.
    public Integer getItemCount(Long userId) {
        return cartRepository.findByUserId(userId).map(cart -> cart.getItems().size()).orElse(0);
    }
}
