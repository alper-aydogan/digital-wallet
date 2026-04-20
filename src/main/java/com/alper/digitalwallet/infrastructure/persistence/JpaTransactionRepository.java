package com.alper.digitalwallet.infrastructure.persistence;

import com.alper.digitalwallet.domain.model.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface JpaTransactionRepository extends JpaRepository<Transaction, Long> {
	List<Transaction> findAllByToWalletId(Long toWalletId);

	Page<Transaction> findAllByToWalletId(Long toWalletId, Pageable pageable);

	@Query("SELECT t FROM Transaction t WHERE t.fromWalletId = :walletId OR t.toWalletId = :walletId")
	Page<Transaction> findAllByFromWalletIdOrToWalletId(@Param("walletId") Long walletId, Pageable pageable);

	Optional<Transaction> findByIdempotencyKey(String idempotencyKey);
}

