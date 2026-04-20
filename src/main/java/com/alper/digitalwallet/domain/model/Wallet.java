package com.alper.digitalwallet.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "wallets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId; // Cüzdanın sahibi kim?

    private BigDecimal balance; // Ne kadar parası var?

    private String currency; // Para birimi ne? (TRY, USD vb.)
}

