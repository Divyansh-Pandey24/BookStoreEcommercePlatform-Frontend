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

    // Retrieves the shopping cart details for the authenticated user.
    @GetMapping
    public ResponseEntity<CartResponse> getCart(@RequestHeader("X-User-Id") Long userId) {
        log.info("Fetching cart for user: {}", userId);
        return ResponseEntity.ok(cartService.getCartByUser(userId));
    }

    // Adds a book catalog item to the user's active shopping cart.
    @PostMapping("/add")
    public ResponseEntity<CartResponse> addItem(@RequestHeader("X-User-Id") Long userId, @Valid @RequestBody AddToCartRequest request) {
        log.info("Adding book {} to cart for user: {}", request.getBookId(), userId);
        return ResponseEntity.ok(cartService.addItem(userId, request));
    }

    // Deletes a specific line item from the shopping cart.
    @DeleteMapping("/item/{itemId}")
    public ResponseEntity<CartResponse> removeItem(@RequestHeader("X-User-Id") Long userId, @PathVariable Long itemId) {
        log.info("Removing item {} from cart for user: {}", itemId, userId);
        return ResponseEntity.ok(cartService.removeItem(userId, itemId));
    }

    // Updates the item quantity count in the user's active cart.
    @PatchMapping("/item/{itemId}")
    public ResponseEntity<CartResponse> updateQuantity(@RequestHeader("X-User-Id") Long userId, @PathVariable Long itemId, @RequestParam Integer quantity) {
        log.info("Updating item {} quantity to {} for user: {}", itemId, quantity, userId);
        return ResponseEntity.ok(cartService.updateQuantity(userId, itemId, quantity));
    }

    // Completely empties the shopping cart.
    @DeleteMapping("/clear")
    public ResponseEntity<String> clearCart(@RequestHeader("X-User-Id") Long userId) {
        log.info("Clearing cart for user: {}", userId);
        cartService.clearCart(userId);
        return ResponseEntity.ok("Cart cleared.");
    }

    // Fetches the unique number of items currently in the cart.
    @GetMapping("/count")
    public ResponseEntity<Integer> getCount(@RequestHeader("X-User-Id") Long userId) {
        log.info("Fetching item count for user: {}", userId);
        return ResponseEntity.ok(cartService.getItemCount(userId));
    }
}
