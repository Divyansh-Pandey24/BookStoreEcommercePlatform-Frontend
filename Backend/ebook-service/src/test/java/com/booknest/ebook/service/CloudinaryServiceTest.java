package com.booknest.ebook.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CloudinaryServiceTest {

    @Mock
    private Cloudinary cloudinary;

    @Mock
    private Uploader uploader;

    @InjectMocks
    private CloudinaryService cloudinaryService;

    @BeforeEach
    void setUp() {
        when(cloudinary.uploader()).thenReturn(uploader);
    }

    @Test
    void testUploadPdf() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getBytes()).thenReturn(new byte[]{1, 2, 3});
        
        Map<String, Object> result = new HashMap<>();
        result.put("url", "http:// cloudinary.com/pdf");
        
        when(uploader.upload(any(byte[].class), anyMap())).thenReturn(result);
        
        String url = cloudinaryService.uploadPdf(file);
        assertEquals("http:// cloudinary.com/pdf", url);
    }

    @Test
    void testUploadImage() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getBytes()).thenReturn(new byte[]{4, 5, 6});
        
        Map<String, Object> result = new HashMap<>();
        result.put("url", "http:// cloudinary.com/image");
        
        when(uploader.upload(any(byte[].class), anyMap())).thenReturn(result);
        
        String url = cloudinaryService.uploadImage(file);
        assertEquals("http:// cloudinary.com/image", url);
    }
}
