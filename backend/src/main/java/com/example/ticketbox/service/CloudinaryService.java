package com.example.ticketbox.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.example.ticketbox.config.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryService {

    private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList(
            "image/jpeg", "image/jpg", "image/png", "image/webp", "image/gif"
    );

    /**
     * [SECURITY] Magic byte signatures for each allowed MIME type (C1).
     * Validates actual file content rather than trusting the client-supplied Content-Type header.
     * WebP uses the RIFF container header (bytes 0-3) as its magic signature.
     */
    private static final Map<String, byte[]> MAGIC_BYTES = new LinkedHashMap<>();
    static {
        MAGIC_BYTES.put("image/jpeg", new byte[]{(byte)0xFF, (byte)0xD8, (byte)0xFF});
        MAGIC_BYTES.put("image/jpg",  new byte[]{(byte)0xFF, (byte)0xD8, (byte)0xFF});
        MAGIC_BYTES.put("image/png",  new byte[]{(byte)0x89, 0x50, 0x4E, 0x47});
        MAGIC_BYTES.put("image/gif",  new byte[]{0x47, 0x49, 0x46, 0x38});
        MAGIC_BYTES.put("image/webp", new byte[]{0x52, 0x49, 0x46, 0x46}); // RIFF
    }

    private final Cloudinary cloudinary;
    private final AppProperties appProperties;

    public String uploadImage(MultipartFile file, String folder) {
        validateFile(file);
        try {
            Map<?, ?> result = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", "ticketbox/" + folder,
                            "resource_type", "image"
                    )
            );
            String url = (String) result.get("secure_url");
            log.info("Uploaded image to Cloudinary: {}", url);
            return url;
        } catch (IOException e) {
            // [SECURITY] Never expose internal error details to caller (C3)
            log.error("Failed to upload image to Cloudinary", e);
            throw new RuntimeException("Upload ảnh thất bại");
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File không được để trống");
        }
        long maxBytes = (long) appProperties.getUpload().getMaxFileSizeMb() * 1024 * 1024;
        if (file.getSize() > maxBytes) {
            throw new IllegalArgumentException("File không được vượt quá " + appProperties.getUpload().getMaxFileSizeMb() + "MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("Chỉ chấp nhận file ảnh (JPEG, PNG, WebP, GIF)");
        }
        // [SECURITY] Validate actual file magic bytes — prevents MIME-type spoofing (C1).
        // A client can claim any Content-Type; reading the real header bytes defeats this.
        validateMagicBytes(file, contentType.toLowerCase());
    }

    /**
     * Read the first 4 bytes of the file and compare against the expected magic signature.
     * Uses InputStream to avoid loading the full file into memory a second time.
     */
    private void validateMagicBytes(MultipartFile file, String contentType) {
        byte[] magic = MAGIC_BYTES.get(contentType);
        if (magic == null) {
            throw new IllegalArgumentException("Chỉ chấp nhận file ảnh (JPEG, PNG, WebP, GIF)");
        }
        try (InputStream is = file.getInputStream()) {
            byte[] header = new byte[magic.length];
            int bytesRead = is.read(header, 0, magic.length);
            if (bytesRead < magic.length || !Arrays.equals(header, magic)) {
                throw new IllegalArgumentException("Nội dung file không hợp lệ. Chỉ chấp nhận file ảnh (JPEG, PNG, WebP, GIF)");
            }
        } catch (IllegalArgumentException e) {
            throw e; // re-throw validation errors as-is
        } catch (IOException e) {
            log.error("Failed to read file header for magic byte validation", e);
            throw new IllegalArgumentException("Không thể đọc file. Vui lòng thử lại.");
        }
    }
}
