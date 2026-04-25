package com.alper.digitalwallet.domain.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

import com.alper.digitalwallet.domain.exception.InsufficientBalanceException;
import com.alper.digitalwallet.domain.exception.InvalidAmountException;

@Entity
@Table(name = "wallets", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"userId"})
})
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    private Long userId;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = true, message = "Bakiye negatif olamaz")
    private BigDecimal balance;

    @NotNull
    private String currency;

    @Version
    private Long version;

    public void debit(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new InvalidAmountException("Tutar sifirdan buyuk olmalidir!");
        }
        if (this.balance.compareTo(amount) < 0) {
            throw new InsufficientBalanceException("Yetersiz bakiye! Mevcut bakiye: " + this.balance);
        }
        this.balance = this.balance.subtract(amount);
    }

    public void credit(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new InvalidAmountException("Tutar sifirdan buyuk olmalidir!");
        }
        this.balance = this.balance.add(amount);
    }

    public boolean isSameCurrencyAs(Wallet other) {
        if (other == null) {
            return false;
        }
        return this.currency.equals(other.currency);
    }
}

