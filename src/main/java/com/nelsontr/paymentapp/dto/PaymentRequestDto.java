package com.nelsontr.paymentapp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(name = "PaymentRequest", description = "Request payload to create a payment")
public class PaymentRequestDto {
    // Using order number as idempotency key
    @NotBlank
    @Schema(description = "Order number used as an idempotency key", example = "ORD-2025-000123")
    private String orderNumber;

    @NotBlank
    @Schema(description = "Payer's first name", example = "Jane")
    private String firstName;

    @NotBlank
    @Schema(description = "Payer's last name", example = "Doe")
    private String lastName;

    @NotBlank
    @Size(min = 5, max = 5)
    @Schema(description = "ZIP/Postal code", example = "90210", minLength = 5, maxLength = 5)
    private String zipCode;

    @NotBlank
    @Size(min = 16, max = 16)
    @Schema(description = "Primary account number (PAN)", example = "4111111111111111", minLength = 16, maxLength = 16)
    private String cardNumber;
}
