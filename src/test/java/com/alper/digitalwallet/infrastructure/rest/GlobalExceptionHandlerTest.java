package com.alper.digitalwallet.infrastructure.rest;

import com.alper.digitalwallet.domain.exception.IdempotencyPayloadMismatchException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleRuntimeException_returns500() {
        RuntimeException ex = new RuntimeException("Unexpected error");
        
        ResponseEntity<ErrorResponse> response = handler.handleRuntimeException(ex);
        
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(500, response.getBody().getStatus());
        assertEquals("INTERNAL_ERROR", response.getBody().getCode());
    }

    @Test
    void handleIdempotencyPayloadMismatchException_returns409() {
        IdempotencyPayloadMismatchException ex = new IdempotencyPayloadMismatchException("Payload mismatch");
        
        ResponseEntity<ErrorResponse> response = handler.handleIdempotencyPayloadMismatch(ex);
        
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(409, response.getBody().getStatus());
        assertEquals("IDEMPOTENCY_PAYLOAD_MISMATCH", response.getBody().getCode());
    }
}
