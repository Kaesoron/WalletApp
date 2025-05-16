package unitTests;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kaesoron.wallet.dto.WalletOperationRequest;
import org.kaesoron.wallet.model.Wallet;
import org.kaesoron.wallet.enums.OperationType;
import org.kaesoron.wallet.exceptions.InsufficientFundsException;
import org.kaesoron.wallet.exceptions.WalletNotFoundException;
import org.kaesoron.wallet.repository.WalletRepository;
import org.kaesoron.wallet.service.WalletService;
import org.mockito.ArgumentCaptor;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class WalletServiceTest {

    private WalletRepository walletRepository;
    private WalletService walletService;

    @BeforeEach
    void setUp() {
        walletRepository = mock(WalletRepository.class);
        walletService = new WalletService(walletRepository);
    }

    @Test
    void deposit_shouldIncreaseBalance() {
        UUID walletId = UUID.randomUUID();
        Wallet wallet = new Wallet(walletId, 1000L);

        when(walletRepository.findByIdForUpdate(walletId)).thenReturn(Optional.of(wallet));

        WalletOperationRequest request = new WalletOperationRequest(walletId, OperationType.DEPOSIT, 500L);

        walletService.processOperation(request);

        ArgumentCaptor<Wallet> captor = ArgumentCaptor.forClass(Wallet.class);
        verify(walletRepository).save(captor.capture());

        assertThat(captor.getValue().getBalance()).isEqualByComparingTo(1500L);
    }

    @Test
    void withdraw_shouldDecreaseBalance() {
        UUID walletId = UUID.randomUUID();
        Wallet wallet = new Wallet(walletId, 1000L);

        when(walletRepository.findByIdForUpdate(walletId)).thenReturn(Optional.of(wallet));

        WalletOperationRequest request = new WalletOperationRequest(walletId, OperationType.WITHDRAW, 400L);

        walletService.processOperation(request);

        ArgumentCaptor<Wallet> captor = ArgumentCaptor.forClass(Wallet.class);
        verify(walletRepository).save(captor.capture());

        assertThat(captor.getValue().getBalance()).isEqualByComparingTo(600L);
    }

    @Test
    void withdraw_shouldThrowException_whenInsufficientFunds() {
        UUID walletId = UUID.randomUUID();
        Wallet wallet = new Wallet(walletId, 100L);

        when(walletRepository.findByIdForUpdate(walletId)).thenReturn(Optional.of(wallet));

        WalletOperationRequest request = new WalletOperationRequest(walletId, OperationType.WITHDRAW, 200L);

        assertThatThrownBy(() -> walletService.processOperation(request))
                .isInstanceOf(InsufficientFundsException.class)
                .hasMessageContaining("Insufficient funds");
    }

    @Test
    void shouldThrowException_whenWalletNotFound() {
        UUID walletId = UUID.randomUUID();

        when(walletRepository.findByIdForUpdate(walletId)).thenReturn(Optional.empty());

        WalletOperationRequest request = new WalletOperationRequest(walletId, OperationType.DEPOSIT, 100L);

        assertThatThrownBy(() -> walletService.processOperation(request))
                .isInstanceOf(WalletNotFoundException.class);
    }

    @Test
    void shouldThrowException_whenUnsupportedOperation() {
        UUID walletId = UUID.randomUUID();
        Wallet wallet = new Wallet(walletId, 1000L);

        when(walletRepository.findByIdForUpdate(walletId)).thenReturn(Optional.of(wallet));

        WalletOperationRequest request = new WalletOperationRequest(walletId, null, 100L);

        assertThatThrownBy(() -> walletService.processOperation(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Operation type must be provided");
    }
}
