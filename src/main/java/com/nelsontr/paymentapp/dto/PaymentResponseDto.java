package com.nelsontr.paymentapp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "PaymentResponse", description = "Response returned after creating or querying a payment")
public class PaymentResponseDto {
    @Schema(description = "Unique transaction identifier", example = "1001")
    private Long transactionId;

    @Schema(description = "Order number used as an idempotency key", example = "ORD-2025-000123")
    private String orderNumber;

    @Schema(description = "Current status of the payment", example = "PENDING", allowableValues = {"PENDING", "APPROVED", "DECLINED"})
    private String status;
}
