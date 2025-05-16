package org.kaesoron.wallet.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.kaesoron.wallet.exceptions.WalletNotFoundException;
import org.kaesoron.wallet.dto.WalletOperationRequest;
import org.kaesoron.wallet.model.Wallet;
import org.kaesoron.wallet.repository.WalletRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;

    @Transactional
    public void processOperation(WalletOperationRequest request) {
        Wallet wallet = walletRepository.findByIdForUpdate(request.getWalletId())
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found: " + request.getWalletId()));

        if (request.getOperationType() == null) {
            throw new IllegalArgumentException("Operation type must be provided");
        }

        switch (request.getOperationType()) {
            case DEPOSIT -> wallet.deposit(request.getAmount());
            case WITHDRAW -> wallet.withdraw(request.getAmount());
            default -> throw new IllegalArgumentException("Unsupported operation: " + request.getOperationType());
        }

        walletRepository.save(wallet); // Не обязателен, Hibernate dirty-checking обновит, но пусть будет явно.
    }

    @Transactional
    public long getBalance(UUID walletId) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found: " + walletId));

        return wallet.getBalance();
    }
}