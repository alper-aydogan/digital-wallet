package com.alper.digitalwallet.infrastructure.persistence;

import com.alper.digitalwallet.domain.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaTransactionRepository extends JpaRepository<Transaction, Long> {
	List<Transaction> findAllByToWalletId(Long toWalletId);
}

