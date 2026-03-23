package com.example.ticketbox.controller;

import com.example.ticketbox.common.ApiResponse;
import com.example.ticketbox.dto.TicketTransferResponse;
import com.example.ticketbox.dto.TransferTicketRequest;
import com.example.ticketbox.security.UserDetailsImpl;
import com.example.ticketbox.service.TicketTransferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class TicketTransferController {

    private final TicketTransferService transferService;

    /** Initiate a transfer — authenticated ticket owner */
    @PostMapping("/v1/tickets/{id}/transfer")
    public ResponseEntity<ApiResponse<TicketTransferResponse>> initiateTransfer(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody TransferTicketRequest request) {
        TicketTransferResponse resp = transferService.initiateTransfer(id, userDetails.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(resp));
    }

    /** Get transfer details by token (accept page) */
    @GetMapping("/v1/ticket-transfers/{token}")
    public ResponseEntity<ApiResponse<TicketTransferResponse>> getByToken(@PathVariable String token) {
        return ResponseEntity.ok(ApiResponse.success(transferService.getByToken(token)));
    }

    /** Accept transfer — recipient must be authenticated */
    @PostMapping("/v1/ticket-transfers/{token}/accept")
    public ResponseEntity<ApiResponse<TicketTransferResponse>> acceptTransfer(
            @PathVariable String token,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        TicketTransferResponse resp = transferService.acceptTransfer(token, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success("Chuyển nhượng thành công", resp));
    }

    /** Cancel transfer — sender only */
    @DeleteMapping("/v1/ticket-transfers/{id}")
    public ResponseEntity<ApiResponse<Void>> cancelTransfer(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        transferService.cancelTransfer(id, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success("Đã hủy yêu cầu chuyển nhượng", null));
    }

    /** List my outgoing transfers */
    @GetMapping("/v1/ticket-transfers/my")
    public ResponseEntity<ApiResponse<List<TicketTransferResponse>>> getMyTransfers(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(ApiResponse.success(transferService.getMyTransfers(userDetails.getId())));
    }
}
