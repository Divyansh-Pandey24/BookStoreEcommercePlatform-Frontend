package com.booknest.ebook.controller;

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

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("EBookPublicController Integration Tests")
class EBookPublicControllerTest {

    private MockMvc mockMvc;

    @Mock private EBookService ebookService;

    @InjectMocks
    private EBookPublicController ebookPublicController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(ebookPublicController).build();
    }

    @Test
    @DisplayName("GET /ebooks/public: success → 200 OK")
    void getActiveEBooks_success() throws Exception {
        when(ebookService.getActiveEBooks()).thenReturn(new ArrayList<>());

        mockMvc.perform(get("/ebooks/public"))
                .andExpect(status().isOk());
    }
}
