package com.alper.digitalwallet.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateWalletRequest {
    @NotNull(message = "Kullanici ID bos olamaz")
    private Long userId;

    @NotBlank(message = "Para birimi bos olamaz")
    @Pattern(regexp = "[A-Z]{3}", message = "Para birimi 3 büyük harf olmalidir (örn: TRY, USD, EUR)")
    private String currency;
}

