package com.alper.digitalwallet.domain.repository;

import com.alper.digitalwallet.domain.model.IdempotencyKey;
import com.alper.digitalwallet.domain.model.IdempotencyKeyStatus;

import java.util.Optional;

public interface IdempotencyKeyRepository {
    IdempotencyKey save(IdempotencyKey idempotencyKey);

    Optional<IdempotencyKey> findByKey(String key);
}
