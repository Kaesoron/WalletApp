package unitTests;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.kaesoron.wallet.controller.WalletController;
import org.kaesoron.wallet.dto.WalletBalanceResponse;
import org.kaesoron.wallet.dto.WalletOperationRequest;
import org.kaesoron.wallet.enums.OperationType;
import org.kaesoron.wallet.exceptions.InsufficientFundsException;
import org.kaesoron.wallet.exceptions.WalletNotFoundException;
import org.kaesoron.wallet.service.WalletService;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class WalletControllerTest {

    private WalletService walletService;
    private WalletController walletController;

    @BeforeEach
    void setUp() {
        walletService = mock(WalletService.class);
        walletController = new WalletController(walletService);
    }

    @Test
    void shouldReturnWalletBalanceResponse() {
        // Given
        UUID walletId = UUID.randomUUID();
        long balance = 1234L;

        when(walletService.getBalance(walletId)).thenReturn(balance);

        // When
        WalletBalanceResponse response = walletController.getBalance(walletId);

        // Then
        assertThat(response.walletId()).isEqualTo(walletId);
        assertThat(response.balance()).isEqualTo(balance);

        verify(walletService).getBalance(walletId);
        verifyNoMoreInteractions(walletService);
    }

    @Test
    void shouldThrowWalletNotFoundException() {
        UUID walletId = UUID.randomUUID();

        doThrow(new WalletNotFoundException("Wallet not found"))
                .when(walletService).getBalance(walletId);

        Executable executable = () -> walletController.getBalance(walletId);

        WalletNotFoundException ex = assertThrows(WalletNotFoundException.class, executable);
        assertThat(ex.getMessage()).isEqualTo("Wallet not found");
    }

    @Test
    void shouldThrowInsufficientFundsException_onProcessOperation() {
        WalletOperationRequest request = new WalletOperationRequest(
                UUID.randomUUID(), OperationType.WITHDRAW, 100L);

        doThrow(new InsufficientFundsException("Not enough funds"))
                .when(walletService).processOperation(request);

        Executable executable = () -> walletController.operate(request);

        InsufficientFundsException ex = assertThrows(InsufficientFundsException.class, executable);
        assertThat(ex.getMessage()).isEqualTo("Not enough funds");
    }
}
