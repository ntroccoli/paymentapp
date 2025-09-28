package com.nelsontr.paymentapp.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class WebhookRequestDto {
    @NotBlank
    private String url;
}
