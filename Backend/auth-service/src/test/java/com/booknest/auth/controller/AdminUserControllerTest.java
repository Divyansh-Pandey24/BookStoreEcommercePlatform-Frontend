package com.booknest.auth.controller;

import com.booknest.auth.entity.User;
import com.booknest.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminUserControllerTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AdminUserController adminUserController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getAllUsers_Success() {
        User user1 = new User();
        user1.setUserId(1L);
        User user2 = new User();
        user2.setUserId(2L);
        when(userRepository.findAll()).thenReturn(Arrays.asList(user1, user2));

        ResponseEntity<List<User>> response = adminUserController.getAllUsers("ADMIN");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(2, response.getBody().size());
        verify(userRepository, times(1)).findAll();
    }

    @Test
    void getAllUsers_Forbidden() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> adminUserController.getAllUsers("CUSTOMER"));
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

    @Test
    void changeRole_Success() {
        User user = new User();
        user.setUserId(1L);
        user.setEmail("user@example.com");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        ResponseEntity<Map<String, String>> response = adminUserController.changeRole("ADMIN", 1L, Map.of("role", "ADMIN"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("User role updated to ADMIN", response.getBody().get("message"));
        assertEquals("ADMIN", user.getRole());
        verify(userRepository, times(1)).save(user);
    }

    @Test
    void changeRole_SuperAdminForbidden() {
        User user = new User();
        user.setUserId(1L);
        user.setEmail("divyanshpandey996@gmail.com");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        Map<String, String> request = Map.of("role", "CUSTOMER");
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> adminUserController.changeRole("ADMIN", 1L, request));
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

    @Test
    void changeRole_InvalidRole() {
        Map<String, String> request = Map.of("role", "INVALID");
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> adminUserController.changeRole("ADMIN", 1L, request));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void suspendUser_Success() {
        User user = new User();
        user.setUserId(1L);
        user.setEmail("user@example.com");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        ResponseEntity<Map<String, String>> response = adminUserController.suspendUser("ADMIN", 1L, Map.of("suspended", true));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("User account suspended successfully", response.getBody().get("message"));
        assertTrue(user.getSuspended());
        verify(userRepository, times(1)).save(user);
    }

    @Test
    void deleteUser_Success() {
        User user = new User();
        user.setUserId(1L);
        user.setEmail("user@example.com");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        ResponseEntity<Map<String, String>> response = adminUserController.deleteUser("ADMIN", 1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("User permanently deleted", response.getBody().get("message"));
        verify(userRepository, times(1)).deleteById(1L);
    }

    @Test
    void getProtectedUser_NotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> adminUserController.deleteUser("ADMIN", 1L));
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }
}
