package com.booknest.ebook.controller;

import com.booknest.ebook.entity.EBook;
import com.booknest.ebook.service.EBookService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.ArrayList;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EBookUserController Integration Tests")
class EBookUserControllerTest {

    private MockMvc mockMvc;

    @Mock private EBookService ebookService;

    @InjectMocks
    private EBookUserController ebookUserController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(ebookUserController).build();
    }

    @Test
    @DisplayName("GET /ebooks/user/{ebookId}: success → 200 OK")
    void getEBook_success() throws Exception {
        EBook ebook = new EBook();
        ebook.setId(1L);
        when(ebookService.getEBook(1L)).thenReturn(ebook);

        mockMvc.perform(get("/ebooks/user/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    @DisplayName("POST /ebooks/user/{ebookId}/purchase: success → 200 OK")
    void purchaseEBook_success() throws Exception {
        mockMvc.perform(post("/ebooks/user/1/purchase")
                        .header("X-User-Id", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("EBook purchased successfully"));
    }

    @Test
    @DisplayName("GET /ebooks/user/{ebookId}/read: success → 200 OK")
    void readEBook_success() throws Exception {
        when(ebookService.getPdfUrlForUser(anyLong(), anyString(), anyLong())).thenReturn("http://pdfurl.com");

        mockMvc.perform(get("/ebooks/user/1/read")
                        .header("X-User-Id", 10L)
                        .header("X-User-Role", "USER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pdfUrl").value("http://pdfurl.com"));
    }

    @Test
    @DisplayName("GET /ebooks/user/purchases: success → 200 OK")
    void getMyPurchases_success() throws Exception {
        when(ebookService.getUserPurchases(10L)).thenReturn(new ArrayList<>());

        mockMvc.perform(get("/ebooks/user/purchases")
                        .header("X-User-Id", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
