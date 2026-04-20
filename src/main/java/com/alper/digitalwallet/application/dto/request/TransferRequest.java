package com.alper.digitalwallet.application.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class TransferRequest {

    @NotNull(message = "Alan kullanici ID bos olamaz")
    private Long toUserId;

    @NotNull(message = "Tutar bos olamaz")
    @DecimalMin(value = "0.01", message = "Transfer tutari en az 0.01 olmalidir")
    private BigDecimal amount;
}

