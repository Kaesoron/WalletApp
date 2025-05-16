package org.kaesoron.wallet.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.kaesoron.wallet.exceptions.InsufficientFundsException;

import java.util.UUID;

@Entity
@Table(name = "wallets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Wallet {

    @Id
    private UUID id;

    @Column(nullable = false)
    private long balance;

    public void deposit(long amount) {
        this.balance += amount;
    }

    public void withdraw(long amount) {
        if (amount > balance) {
            throw new InsufficientFundsException("Insufficient funds: requested " + amount + ", available " + balance);
        }
        this.balance -= amount;
    }
}
