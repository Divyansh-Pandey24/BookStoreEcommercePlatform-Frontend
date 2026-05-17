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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.ArrayList;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EBookAdminController Integration Tests")
class EBookAdminControllerTest {

    private MockMvc mockMvc;

    @Mock private EBookService ebookService;

    @InjectMocks
    private EBookAdminController ebookAdminController;

    private EBook sampleEBook;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(ebookAdminController).build();
        sampleEBook = new EBook();
        sampleEBook.setId(1L);
        sampleEBook.setTitle("Sample EBook");
    }

    @Test
    @DisplayName("GET /ebooks/admin: success → 200 OK")
    void getAllEBooks_success() throws Exception {
        when(ebookService.getAllEBooks()).thenReturn(new ArrayList<>());

        mockMvc.perform(get("/ebooks/admin")
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /ebooks/admin: forbidden → 403 Forbidden")
    void getAllEBooks_forbidden() throws Exception {
        mockMvc.perform(get("/ebooks/admin")
                        .header("X-User-Role", "USER"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /ebooks/admin: success → 200 OK")
    void createEBook_success() throws Exception {
        MockMultipartFile pdfFile = new MockMultipartFile("pdfFile", "test.pdf", "application/pdf", "test data".getBytes());
        
        when(ebookService.createEBook(anyString(), anyString(), anyString(), anyDouble(), any(), any())).thenReturn(sampleEBook);

        mockMvc.perform(multipart("/ebooks/admin")
                        .file(pdfFile)
                        .param("title", "New EBook")
                        .param("author", "Author")
                        .param("description", "Desc")
                        .param("price", "10.0")
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }
}
