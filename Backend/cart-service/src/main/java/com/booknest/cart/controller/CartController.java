package com.booknest.cart.controller;

import com.booknest.cart.dto.AddToCartRequest;
import com.booknest.cart.dto.CartResponse;
import com.booknest.cart.service.CartServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// Controller for managing user shopping carts
@Slf4j
@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartServiceImpl cartService;

    // Retrieve the shopping cart for the authenticated user
    @GetMapping
    public ResponseEntity<CartResponse> getCart(@RequestHeader("X-User-Id") Long userId) {
        log.info("Fetching cart for user: {}", userId);
        return ResponseEntity.ok(cartService.getCartByUser(userId));
    }

    // Add a book to the authenticated user's cart
    @PostMapping("/add")
    public ResponseEntity<CartResponse> addItem(@RequestHeader("X-User-Id") Long userId, @Valid @RequestBody AddToCartRequest request) {
        log.info("Adding book {} to cart for user: {}", request.getBookId(), userId);
        return ResponseEntity.ok(cartService.addItem(userId, request));
    }

    // Remove an item from the authenticated user's cart
    @DeleteMapping("/item/{itemId}")
    public ResponseEntity<CartResponse> removeItem(@RequestHeader("X-User-Id") Long userId, @PathVariable Long itemId) {
        log.info("Removing item {} from cart for user: {}", itemId, userId);
        return ResponseEntity.ok(cartService.removeItem(userId, itemId));
    }

    // Update the quantity of an item in the user's cart
    @PatchMapping("/item/{itemId}")
    public ResponseEntity<CartResponse> updateQuantity(@RequestHeader("X-User-Id") Long userId, @PathVariable Long itemId, @RequestParam Integer quantity) {
        log.info("Updating item {} quantity to {} for user: {}", itemId, quantity, userId);
        return ResponseEntity.ok(cartService.updateQuantity(userId, itemId, quantity));
    }

    // Clear all items from the user's cart
    @DeleteMapping("/clear")
    public ResponseEntity<String> clearCart(@RequestHeader("X-User-Id") Long userId) {
        log.info("Clearing cart for user: {}", userId);
        cartService.clearCart(userId);
        return ResponseEntity.ok("Cart cleared.");
    }

    // Get the total count of items in the user's cart
    @GetMapping("/count")
    public ResponseEntity<Integer> getCount(@RequestHeader("X-User-Id") Long userId) {
        log.info("Fetching item count for user: {}", userId);
        return ResponseEntity.ok(cartService.getItemCount(userId));
    }
}