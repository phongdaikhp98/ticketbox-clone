package com.example.ticketbox.controller;

import com.example.ticketbox.common.ApiResponse;
import com.example.ticketbox.dto.WishlistCheckResponse;
import com.example.ticketbox.dto.WishlistResponse;
import com.example.ticketbox.security.UserDetailsImpl;
import com.example.ticketbox.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/wishlists")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<WishlistResponse>>> getMyWishlist(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<WishlistResponse> wishlists = wishlistService.getMyWishlist(userDetails.getId(), page, size);
        return ResponseEntity.ok(ApiResponse.success(wishlists));
    }

    @PostMapping("/{eventId}")
    public ResponseEntity<ApiResponse<WishlistResponse>> addToWishlist(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long eventId) {
        WishlistResponse wishlist = wishlistService.addToWishlist(userDetails.getId(), eventId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(wishlist));
    }

    @DeleteMapping("/{eventId}")
    public ResponseEntity<ApiResponse<Void>> removeFromWishlist(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long eventId) {
        wishlistService.removeFromWishlist(userDetails.getId(), eventId);
        return ResponseEntity.ok(ApiResponse.success("Removed from wishlist", null));
    }

    @GetMapping("/{eventId}/check")
    public ResponseEntity<ApiResponse<WishlistCheckResponse>> checkWishlist(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long eventId) {
        WishlistCheckResponse response = wishlistService.checkWishlist(userDetails.getId(), eventId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
