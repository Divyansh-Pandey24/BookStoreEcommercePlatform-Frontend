package com.booknest.ebook.service;

import com.booknest.ebook.client.WalletClient;
import com.booknest.ebook.entity.EBook;
import com.booknest.ebook.entity.EBookPurchase;
import com.booknest.ebook.repository.EBookPurchaseRepository;
import com.booknest.ebook.repository.EBookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EBookServiceTest {

    @Mock
    private EBookRepository ebookRepository;

    @Mock
    private EBookPurchaseRepository purchaseRepository;

    @Mock
    private CloudinaryService cloudinaryService;

    @Mock
    private WalletClient walletClient;

    @InjectMocks
    private EBookService ebookService;

    private EBook sampleEBook;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        sampleEBook = new EBook();
        sampleEBook.setId(1L);
        sampleEBook.setTitle("Test Book");
        sampleEBook.setAuthor("Test Author");
        sampleEBook.setPrice(100.0);
        sampleEBook.setPdfUrl("/ebook-uploads/test.pdf");
        sampleEBook.setActive(true);

        ReflectionTestUtils.setField(ebookService, "uploadDir", tempDir.toAbsolutePath().toString());
        ReflectionTestUtils.setField(ebookService, "gatewaySecret", "secret");
    }

    @Test
    void testGetActiveEBooks() {
        when(ebookRepository.findByActiveTrue()).thenReturn(Arrays.asList(sampleEBook));
        List<EBook> books = ebookService.getActiveEBooks();
        assertEquals(1, books.size());
        assertTrue(books.get(0).getActive());
    }

    @Test
    void testGetAllEBooks() {
        when(ebookRepository.findAll()).thenReturn(Arrays.asList(sampleEBook));
        List<EBook> books = ebookService.getAllEBooks();
        assertEquals(1, books.size());
    }

    @Test
    void testGetEBook_Success() {
        when(ebookRepository.findById(1L)).thenReturn(Optional.of(sampleEBook));
        EBook book = ebookService.getEBook(1L);
        assertNotNull(book);
        assertEquals("Test Book", book.getTitle());
    }

    @Test
    void testGetEBook_NotFound() {
        when(ebookRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResponseStatusException.class, () -> ebookService.getEBook(1L));
    }

    @Test
    void testPurchaseEBook_Success() {
        when(purchaseRepository.existsByUserIdAndEbookId(anyLong(), anyLong())).thenReturn(false);
        when(ebookRepository.findById(1L)).thenReturn(Optional.of(sampleEBook));
        
        ebookService.purchaseEBook(1L, 1L);
        
        verify(walletClient, times(1)).deductMoney(anyString(), anyLong(), anyDouble(), any());
        verify(purchaseRepository, times(1)).save(any(EBookPurchase.class));
    }

    @Test
    void testPurchaseEBook_WalletFail() {
        when(purchaseRepository.existsByUserIdAndEbookId(anyLong(), anyLong())).thenReturn(false);
        when(ebookRepository.findById(1L)).thenReturn(Optional.of(sampleEBook));
        doThrow(new RuntimeException("Insufficient funds")).when(walletClient).deductMoney(any(), any(), any(), any());
        
        assertThrows(ResponseStatusException.class, () -> ebookService.purchaseEBook(1L, 1L));
    }

    @Test
    void testPurchaseEBook_AlreadyPurchased() {
        when(purchaseRepository.existsByUserIdAndEbookId(1L, 1L)).thenReturn(true);
        assertThrows(ResponseStatusException.class, () -> ebookService.purchaseEBook(1L, 1L));
    }

    @Test
    void testGetPdfUrlForUser_Admin() {
        when(ebookRepository.findById(1L)).thenReturn(Optional.of(sampleEBook));
        String url = ebookService.getPdfUrlForUser(1L, "ADMIN", 1L);
        assertEquals("/ebook-uploads/test.pdf", url);
    }

    @Test
    void testGetPdfUrlForUser_PurchasedUser() {
        when(purchaseRepository.existsByUserIdAndEbookId(1L, 1L)).thenReturn(true);
        when(ebookRepository.findById(1L)).thenReturn(Optional.of(sampleEBook));
        String url = ebookService.getPdfUrlForUser(1L, "CUSTOMER", 1L);
        assertEquals("/ebook-uploads/test.pdf", url);
    }

    @Test
    void testGetPdfUrlForUser_Forbidden() {
        when(purchaseRepository.existsByUserIdAndEbookId(1L, 1L)).thenReturn(false);
        assertThrows(ResponseStatusException.class, () -> ebookService.getPdfUrlForUser(1L, "CUSTOMER", 1L));
    }

    @Test
    void testGetPurchases() {
        when(purchaseRepository.findAll()).thenReturn(Collections.emptyList());
        assertEquals(0, ebookService.getPurchases().size());
    }

    @Test
    void testGetUserPurchases() {
        when(purchaseRepository.findByUserId(1L)).thenReturn(Collections.emptyList());
        assertEquals(0, ebookService.getUserPurchases(1L).size());
    }

    @Test
    void testCreateEBook() throws IOException {
        MockMultipartFile pdfFile = new MockMultipartFile("pdf", "test.pdf", "application/pdf", "pdfcontent".getBytes());
        MockMultipartFile coverFile = new MockMultipartFile("cover", "test.png", "image/png", "imgcontent".getBytes());
        
        when(ebookRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        EBook result = ebookService.createEBook("New Title", "New Author", "Desc", 50.0, pdfFile, coverFile);

        assertNotNull(result);
        assertEquals("New Title", result.getTitle());
        assertTrue(result.getPdfUrl().startsWith("/ebook-uploads/"));
        assertTrue(result.getCoverImageUrl().startsWith("/ebook-uploads/"));
    }

    @Test
    void testUpdateEBook() throws IOException {
        MockMultipartFile pdfFile = new MockMultipartFile("pdf", "updated.pdf", "application/pdf", "pdfcontent".getBytes());
        MockMultipartFile coverFile = new MockMultipartFile("cover", "updated.png", "image/png", "imgcontent".getBytes());
        
        when(ebookRepository.findById(1L)).thenReturn(Optional.of(sampleEBook));
        when(ebookRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        EBook result = ebookService.updateEBook(1L, "Updated Title", "Updated Author", "Updated Desc", 75.0, pdfFile, coverFile);

        assertEquals("Updated Title", result.getTitle());
        assertEquals("Updated Author", result.getAuthor());
        assertEquals(75.0, result.getPrice());
        assertTrue(result.getPdfUrl().contains("updated.pdf"));
    }

    @Test
    void testDeleteEBook() {
        when(ebookRepository.findById(1L)).thenReturn(Optional.of(sampleEBook));
        
        ebookService.deleteEBook(1L);
        
        verify(purchaseRepository, times(1)).deleteByEbookId(1L);
        verify(ebookRepository, times(1)).deleteById(1L);
    }
}
