package com.booknest.auth.controller;

import com.booknest.auth.entity.User;
import com.booknest.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

/**
 * Admin-only endpoints for user management.
 * All endpoints are protected — only ADMIN role can access them.
 * Super admin (divyanshpandey996@gmail.com) is permanently protected from any modification.
 */
@RestController
@RequestMapping("/auth/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserRepository userRepository;

    // The email address of the super admin — can never be modified or deleted
    private static final String SUPER_ADMIN_EMAIL = "divyanshpandey996@gmail.com";

    private void checkAdmin(String role) {
        if (!"ADMIN".equals(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Admins only");
        }
    }

    private User getProtectedUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (SUPER_ADMIN_EMAIL.equals(user.getEmail())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This super admin account cannot be modified.");
        }
        return user;
    }

    /** List all users */
    @GetMapping
    public ResponseEntity<List<User>> getAllUsers(@RequestHeader("X-User-Role") String role) {
        checkAdmin(role);
        return ResponseEntity.ok(userRepository.findAll());
    }

    /** Promote a user to ADMIN or demote to CUSTOMER */
    @PutMapping("/{userId}/role")
    public ResponseEntity<Map<String, String>> changeRole(
            @RequestHeader("X-User-Role") String role,
            @PathVariable Long userId,
            @RequestBody Map<String, String> body) {

        checkAdmin(role);
        String newRole = body.get("role");
        if (newRole == null || (!newRole.equals("ADMIN") && !newRole.equals("CUSTOMER"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role must be ADMIN or CUSTOMER");
        }

        User user = getProtectedUser(userId);
        user.setRole(newRole);
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("message", "User role updated to " + newRole));
    }

    /** Soft-delete: suspend a user (they cannot login but data is kept) */
    @PutMapping("/{userId}/suspend")
    public ResponseEntity<Map<String, String>> suspendUser(
            @RequestHeader("X-User-Role") String role,
            @PathVariable Long userId,
            @RequestBody Map<String, Boolean> body) {

        checkAdmin(role);
        User user = getProtectedUser(userId);
        boolean suspend = Boolean.TRUE.equals(body.get("suspended"));
        user.setSuspended(suspend);
        userRepository.save(user);
        String status = suspend ? "suspended" : "reactivated";
        return ResponseEntity.ok(Map.of("message", "User account " + status + " successfully"));
    }

    /** Hard-delete: permanently remove the user from the database */
    @DeleteMapping("/{userId}")
    public ResponseEntity<Map<String, String>> deleteUser(
            @RequestHeader("X-User-Role") String role,
            @PathVariable Long userId) {

        checkAdmin(role);
        getProtectedUser(userId); // will throw if super admin or not found
        userRepository.deleteById(userId);
        return ResponseEntity.ok(Map.of("message", "User permanently deleted"));
    }
}
