package org.kaesoron.wallet.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.kaesoron.wallet.enums.OperationType;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WalletOperationRequest {

    @NotNull
    private UUID walletId;

    @NotNull
    private OperationType operationType;

    @Positive
    private long amount;
}