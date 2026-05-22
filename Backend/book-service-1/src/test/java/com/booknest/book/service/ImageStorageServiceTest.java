package com.booknest.book.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ImageStorageServiceTest {

    @Mock
    private Cloudinary cloudinary;

    @Mock
    private Uploader uploader;

    @InjectMocks
    private ImageStorageService imageStorageService;

    @BeforeEach
    void setUp() {
        // Mock the uploader call which is chain-called in the service
        lenient().when(cloudinary.uploader()).thenReturn(uploader);
    }

    @Test
    void saveImage_validFile_savesAndReturnsPath() throws IOException {
        MockMultipartFile mockFile = new MockMultipartFile(
                "file",
                "test-cover.jpg",
                "image/jpeg",
                "dummy image content".getBytes()
        );

        Map<String, Object> uploadResult = Map.of("secure_url", "https:// cloudinary.com/booknest/covers/test.jpg");
        when(uploader.upload(any(byte[].class), anyMap())).thenReturn(uploadResult);

        String savedPath = imageStorageService.saveImage(mockFile);

        assertThat(savedPath).isEqualTo("https:// cloudinary.com/booknest/covers/test.jpg");
        verify(uploader).upload(any(byte[].class), anyMap());
    }

    @Test
    void saveImage_emptyFile_throwsException() {
        MockMultipartFile mockFile = new MockMultipartFile(
                "file",
                "empty.jpg",
                "image/jpeg",
                new byte[0]
        );

        RuntimeException ex = assertThrows(RuntimeException.class, () -> imageStorageService.saveImage(mockFile));
        assertThat(ex.getMessage()).isEqualTo("Please select an image to upload");
    }

    @Test
    void saveImage_invalidExtension_throwsException() {
        MockMultipartFile mockFile = new MockMultipartFile(
                "file",
                "document.pdf",
                "application/pdf",
                "dummy pdf content".getBytes()
        );

        RuntimeException ex = assertThrows(RuntimeException.class, () -> imageStorageService.saveImage(mockFile));
        assertThat(ex.getMessage()).contains("Only image files are allowed");
    }

    @Test
    void saveImage_fileTooLarge_throwsException() {
        byte[] largeBytes = new byte[(5 * 1024 * 1024) + 10]; // Slightly larger than 5MB
        MockMultipartFile mockFile = new MockMultipartFile(
                "file",
                "large.png",
                "image/png",
                largeBytes
        );

        RuntimeException ex = assertThrows(RuntimeException.class, () -> imageStorageService.saveImage(mockFile));
        assertThat(ex.getMessage()).isEqualTo("Image size cannot exceed 5MB");
    }

    @Test
    void deleteImage_validPath_deletesFile() throws IOException {
        String cloudinaryUrl = "https:// res.cloudinary.com/demo/image/upload/v1/booknest/covers/test.webp";
        
        imageStorageService.deleteImage(cloudinaryUrl);

        verify(uploader).destroy(eq("booknest/covers/test"), anyMap());
    }

    @Test
    void deleteImage_nullOrEmptyPath_doesNothing() throws IOException {
        imageStorageService.deleteImage(null);
        imageStorageService.deleteImage("");
        
        verify(uploader, never()).destroy(anyString(), anyMap());
    }

    @Test
    void saveImageFromFile_success() throws IOException {
        java.io.File mockFile = mock(java.io.File.class);
        when(mockFile.getName()).thenReturn("test.jpg");
        Map<String, Object> uploadResult = Map.of("secure_url", "https:// cloudinary.com/booknest/covers/test.jpg");
        when(uploader.upload(any(java.io.File.class), anyMap())).thenReturn(uploadResult);

        String savedPath = imageStorageService.saveImageFromFile(mockFile);

        assertThat(savedPath).isEqualTo("https:// cloudinary.com/booknest/covers/test.jpg");
    }

    @Test
    void saveImageFromFile_throwsException() throws IOException {
        java.io.File mockFile = mock(java.io.File.class);
        when(mockFile.getName()).thenReturn("test.jpg");
        when(uploader.upload(any(java.io.File.class), anyMap())).thenThrow(new IOException("Cloudinary error"));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> imageStorageService.saveImageFromFile(mockFile));
        assertThat(ex.getMessage()).isEqualTo("Failed to migrate image.");
    }

    @Test
    void saveImage_uploadThrowsIOException() throws IOException {
        MockMultipartFile mockFile = new MockMultipartFile(
                "file",
                "test-cover.jpg",
                "image/jpeg",
                "dummy".getBytes()
        );
        when(uploader.upload(any(byte[].class), anyMap())).thenThrow(new IOException("Upload failed"));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> imageStorageService.saveImage(mockFile));
        assertThat(ex.getMessage()).contains("Image upload failed: Upload failed");
    }

    @Test
    void deleteImage_ioException() throws IOException {
        String cloudinaryUrl = "https:// res.cloudinary.com/demo/image/upload/v1/booknest/covers/test.webp";
        when(uploader.destroy(eq("booknest/covers/test"), anyMap())).thenThrow(new IOException("Delete failed"));

        // Should not throw, just logs warning
        imageStorageService.deleteImage(cloudinaryUrl);
        verify(uploader).destroy(eq("booknest/covers/test"), anyMap());
    }

    @Test
    void deleteImage_invalidUrl() throws IOException {
        String invalidUrl = "https:// res.cloudinary.com/demo/image/invalidurl";
        
        imageStorageService.deleteImage(invalidUrl);
        
        verify(uploader, never()).destroy(anyString(), anyMap());
    }

    @Test
    void saveImage_invalidFilename() {
        MockMultipartFile mockFile = new MockMultipartFile(
                "file",
                "",
                "image/jpeg",
                "dummy".getBytes()
        );

        RuntimeException ex = assertThrows(RuntimeException.class, () -> imageStorageService.saveImage(mockFile));
        assertThat(ex.getMessage()).isEqualTo("Invalid file name");
    }
}
