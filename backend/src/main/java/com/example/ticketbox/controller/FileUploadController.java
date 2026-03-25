package com.example.ticketbox.controller;

import com.example.ticketbox.common.ApiResponse;
import com.example.ticketbox.exception.BadRequestException;
import com.example.ticketbox.service.CloudinaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/v1/upload")
@RequiredArgsConstructor
public class FileUploadController {

    /**
     * [SECURITY] Whitelist of permitted Cloudinary sub-folders.
     * Prevents path traversal attacks (e.g. folder=../../admin) via (C2).
     */
    private static final Set<String> ALLOWED_FOLDERS = Set.of("events", "avatars", "general");

    private final CloudinaryService cloudinaryService;

    @PostMapping("/image")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", defaultValue = "general") String folder
    ) {
        // [SECURITY] Reject unknown folder values before they reach Cloudinary (C2)
        if (!ALLOWED_FOLDERS.contains(folder)) {
            throw new BadRequestException("Folder không hợp lệ. Chỉ chấp nhận: events, avatars, general");
        }
        String url = cloudinaryService.uploadImage(file, folder);
        return ResponseEntity.ok(ApiResponse.success("Upload ảnh thành công", Map.of("url", url)));
    }
}
