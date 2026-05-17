package com.booknest.book.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

/**
 * Image storage service backed by Cloudinary.
 *
 * BEFORE: Saved images to local disk under uploads/books/.
 *   Problems:
 *     - Files lost on server restart / redeploy
 *     - Doesn't work with multiple service instances
 *     - Wastes server disk space
 *     - Requires a static resource handler and gateway route
 *
 * AFTER: Uploads images directly to Cloudinary CDN.
 *   Benefits:
 *     - Images are globally accessible via HTTPS CDN URL
 *     - Zero disk usage on the server
 *     - Automatic image optimization and format conversion
 *     - Works seamlessly with multiple instances
 *     - The coverImageUrl stored in DB is a full HTTPS URL —
 *       the frontend's getImageUrl() already handles full URLs correctly.
 *
 * Cloudinary folder: "booknest/covers"
 * All images are uploaded under this folder for easy management.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ImageStorageService {

    private final Cloudinary cloudinary;

    // All book covers are stored under this Cloudinary folder
    private static final String UPLOAD_FOLDER = "booknest/covers";

    /**
     * Uploads a book cover image to Cloudinary and returns the secure URL.
     *
     * @param file the uploaded image file from the HTTP request
     * @return the secure HTTPS URL of the uploaded image on Cloudinary CDN
     */
    public String saveImage(MultipartFile file) {
        log.info("Uploading book cover image to Cloudinary...");

        if (file == null || file.isEmpty()) {
            throw new RuntimeException("Please select an image to upload");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new RuntimeException("Invalid file name");
        }

        // Validate file extension
        String extension = originalFilename
                .substring(originalFilename.lastIndexOf("."))
                .toLowerCase();
        if (!extension.matches("\\.(jpg|jpeg|png|gif|webp)")) {
            throw new RuntimeException("Only image files are allowed (jpg, jpeg, png, gif, webp)");
        }

        // Validate file size (max 5MB)
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new RuntimeException("Image size cannot exceed 5MB");
        }

        try {
            // Upload to Cloudinary
            // Cloudinary auto-assigns a public_id if we don't specify one.
            // We specify the folder so all covers are organised together.
            @SuppressWarnings("unchecked")
            Map<String, Object> uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder",          UPLOAD_FOLDER,
                            "resource_type",   "image",
                            // Eagerly transform to WebP for better compression
                            "format",          "webp",
                            // Limit dimensions — Cloudinary resizes server-side
                            "transformation",  "c_limit,w_800,h_1200,q_auto"
                    )
            );

            // "secure_url" is the HTTPS CDN URL of the uploaded image
            String secureUrl = (String) uploadResult.get("secure_url");
            log.info("Book cover uploaded to Cloudinary: {}", secureUrl);
            return secureUrl;

        } catch (IOException e) {
            log.error("Failed to upload image to Cloudinary. Possible causes: wrong credentials, no internet inside Docker, or firewall.", e);
            throw new RuntimeException("Image upload failed: " + e.getMessage());
        }
    }

    /**
     * Uploads an existing local file to Cloudinary and returns the secure URL.
     * This is specifically used for migrating old local images.
     *
     * @param file the local image file
     * @return the secure HTTPS URL of the uploaded image on Cloudinary CDN
     */
    public String saveImageFromFile(java.io.File file) {
        log.info("Migrating local image to Cloudinary: {}", file.getName());

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> uploadResult = cloudinary.uploader().upload(
                    file,
                    ObjectUtils.asMap(
                            "folder",          UPLOAD_FOLDER,
                            "resource_type",   "image",
                            "format",          "webp",
                            "transformation",  "c_limit,w_800,h_1200,q_auto"
                    )
            );

            String secureUrl = (String) uploadResult.get("secure_url");
            log.info("Successfully migrated image to Cloudinary: {}", secureUrl);
            return secureUrl;

        } catch (IOException e) {
            log.error("Failed to migrate image {} to Cloudinary: {}", file.getName(), e.getMessage());
            throw new RuntimeException("Failed to migrate image.", e);
        }
    }

    /**
     * Deletes an existing image from Cloudinary using its public_id.
     *
     * The stored URL looks like:
     *   https://res.cloudinary.com/{cloud}/image/upload/v123/booknest/covers/{publicId}.webp
     * We extract the public_id portion (everything after /upload/v<version>/).
     *
     * @param imageUrl the full Cloudinary URL stored in the database
     */
    public void deleteImage(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) return;

        // Skip deletion for old local paths (migration period)
        if (!imageUrl.contains("cloudinary.com")) {
            log.info("Skipping deletion of non-Cloudinary image path: {}", imageUrl);
            return;
        }

        try {
            // Extract public_id from the Cloudinary URL
            // URL pattern: .../upload/v<version>/<publicId>.<ext>
            String publicId = extractPublicId(imageUrl);
            if (publicId == null) {
                log.warn("Could not extract public_id from Cloudinary URL: {}", imageUrl);
                return;
            }

            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            log.info("Deleted Cloudinary image: {}", publicId);

        } catch (IOException e) {
            log.warn("Could not delete Cloudinary image: {} — {}", imageUrl, e.getMessage());
        }
    }

    /**
     * Extracts the Cloudinary public_id from a full Cloudinary URL.
     * Example:
     *   Input:  https://res.cloudinary.com/demo/image/upload/v1620000000/booknest/covers/abc123.webp
     *   Output: booknest/covers/abc123
     */
    private String extractPublicId(String url) {
        try {
            // Split on "/upload/" and take the second part
            String[] parts = url.split("/upload/");
            if (parts.length < 2) return null;

            String afterUpload = parts[1]; // e.g., "v1620000000/booknest/covers/abc123.webp"

            // Remove the version segment (v<digits>/)
            if (afterUpload.matches("v\\d+/.*")) {
                afterUpload = afterUpload.replaceFirst("v\\d+/", "");
            }

            // Remove file extension
            int dotIndex = afterUpload.lastIndexOf(".");
            if (dotIndex != -1) {
                afterUpload = afterUpload.substring(0, dotIndex);
            }

            return afterUpload; // e.g., "booknest/covers/abc123"
        } catch (Exception e) {
            log.warn("Error parsing Cloudinary public_id from URL: {}", url);
            return null;
        }
    }
}
