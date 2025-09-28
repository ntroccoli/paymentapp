package com.nelsontr.paymentapp.controller;

import com.nelsontr.paymentapp.dto.PaymentRequestDto;
import com.nelsontr.paymentapp.dto.PaymentResponseDto;
import com.nelsontr.paymentapp.mapper.PaymentMapper;
import com.nelsontr.paymentapp.model.Payment;
import com.nelsontr.paymentapp.service.PaymentService;
import com.nelsontr.paymentapp.exception.ResourceNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Optional;

@RestController
@RequestMapping("/payments")
@Tag(name = "Payments", description = "Create and query payment transactions")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    @Operation(
            operationId = "createPayment",
            summary = "Create a payment",
            description = "Creates a new payment. If a payment with the same orderNumber exists, the existing one is returned (idempotent)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Payment created",
                    headers = {
                            @Header(name = "Location", description = "URI of the created resource",
                                    schema = @Schema(type = "string"))
                    },
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = PaymentResponseDto.class),
                            examples = @ExampleObject(name = "created",
                                    value = "{\n  \"transactionId\": 1001,\n  \"orderNumber\": \"ORD-2025-000123\",\n  \"status\": \"PENDING\"\n}"))) ,
            @ApiResponse(responseCode = "200", description = "Payment already exists (idempotent)",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = PaymentResponseDto.class),
                            examples = @ExampleObject(name = "existing",
                                    value = "{\n  \"transactionId\": 1001,\n  \"orderNumber\": \"ORD-2025-000123\",\n  \"status\": \"PENDING\"\n}"))),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(name = "bad-request",
                                    value = "{\n  \"type\": \"urn:paymentapp:problem:validation-error\",\n  \"title\": \"Bad Request\",\n  \"status\": 400,\n  \"detail\": \"Validation failed for request body\",\n  \"errors\": { \"zipCode\": [\"size must be between 5 and 5\"] }\n}")))
    })
    public ResponseEntity<PaymentResponseDto> createPayment(
            @Valid @RequestBody
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = PaymentRequestDto.class),
                            examples = {
                                    @ExampleObject(name = "valid",
                                            value = "{\n  \"orderNumber\": \"ORD-2025-000123\",\n  \"firstName\": \"Jane\",\n  \"lastName\": \"Doe\",\n  \"zipCode\": \"90210\",\n  \"cardNumber\": \"4111111111111111\"\n}")
                            }
                    )
            )
            PaymentRequestDto request) {
        PaymentService.CreatePaymentResult result = paymentService.createOrGetPayment(request);
        Payment payment = result.getPayment();
        if (result.isCreated()) {
            return ResponseEntity.created(URI.create("/payments/" + payment.getTransactionId()))
                    .body(PaymentMapper.mapToDto(payment));
        }
        return ResponseEntity.ok(PaymentMapper.mapToDto(payment));
    }

    @GetMapping
    @Operation(
            operationId = "getPaymentByOrderNumber",
            summary = "Get payment by order number",
            description = "Returns the payment for the given orderNumber if it exists."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Payment found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = PaymentResponseDto.class),
                            examples = @ExampleObject(value = "{\n  \"transactionId\": 1001,\n  \"orderNumber\": \"ORD-2025-000123\",\n  \"status\": \"PENDING\"\n}"))),
            @ApiResponse(responseCode = "404", description = "Payment not found",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(value = "{\n  \"type\": \"urn:paymentapp:problem:resource-not-found\",\n  \"title\": \"Not Found\",\n  \"status\": 404,\n  \"detail\": \"Payment not found for orderNumber: ORD-2025-000999\"\n}")))
    })
    public ResponseEntity<PaymentResponseDto> getPaymentStatus(
            @Parameter(description = "Order number used as an idempotency key", example = "ORD-2025-000123")
            @RequestParam String orderNumber) {
        Optional<Payment> paymentOpt = paymentService.getPaymentByOrderNumber(orderNumber);

        if (paymentOpt.isEmpty()) {
            throw new ResourceNotFoundException("Payment not found for orderNumber: " + orderNumber);
        }

        return ResponseEntity.ok(PaymentMapper.mapToDto(paymentOpt.get()));
    }

    @GetMapping("/{transactionId}")
    @Operation(operationId = "getPaymentByTransactionId", summary = "Get payment by transaction id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Payment found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = PaymentResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Payment not found",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<PaymentResponseDto> getPaymentStatusByTransactionId(
            @Parameter(description = "Unique transaction identifier", example = "1001")
            @PathVariable Long transactionId) {
        Optional<Payment> paymentOpt = paymentService.getPaymentByTransactionId(transactionId);

        if (paymentOpt.isEmpty()) {
            throw new ResourceNotFoundException("Payment not found for transactionId: " + transactionId);
        }

        return ResponseEntity.ok(PaymentMapper.mapToDto(paymentOpt.get()));
    }

}
