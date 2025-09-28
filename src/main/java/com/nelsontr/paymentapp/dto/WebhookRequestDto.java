package com.nelsontr.paymentapp.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.hibernate.validator.constraints.URL;

@Data
public class WebhookRequestDto {
    @NotBlank
    @URL(regexp = "^(?i)(http|https)://.+$", message = "must be a valid http(s) URL")
    private String url;
}
