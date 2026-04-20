package com.alper.digitalwallet.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateWalletRequest {
    @NotNull(message = "Kullanici ID bos olamaz")
    private Long userId;

    @NotBlank(message = "Para birimi bos olamaz")
    private String currency;
}

