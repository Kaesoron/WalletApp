package org.kaesoron.wallet.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.kaesoron.wallet.dto.WalletOperationRequest;
import org.kaesoron.wallet.service.WalletService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @PostMapping("/wallet")
    public ResponseEntity<Void> operate(@RequestBody @Valid WalletOperationRequest request) {
        walletService.processOperation(request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/wallets/{walletId}")
    public ResponseEntity<Long> getBalance(@PathVariable UUID walletId) {
        long balance = walletService.getBalance(walletId);
        return ResponseEntity.ok(balance);
    }
}
