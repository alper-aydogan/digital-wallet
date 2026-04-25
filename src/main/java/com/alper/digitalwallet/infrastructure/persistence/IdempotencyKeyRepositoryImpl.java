package com.alper.digitalwallet.infrastructure.persistence;

import com.alper.digitalwallet.domain.model.IdempotencyKey;
import com.alper.digitalwallet.domain.repository.IdempotencyKeyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class IdempotencyKeyRepositoryImpl implements IdempotencyKeyRepository {

    private final JpaIdempotencyKeyRepository jpaRepository;

    @Override
    public IdempotencyKey save(IdempotencyKey idempotencyKey) {
        return jpaRepository.save(idempotencyKey);
    }

    @Override
    public Optional<IdempotencyKey> findByKey(String key) {
        return jpaRepository.findByKey(key);
    }
}
