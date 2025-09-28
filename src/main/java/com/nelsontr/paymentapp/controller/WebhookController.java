package com.nelsontr.paymentapp.controller;

import com.nelsontr.paymentapp.dto.PaymentResponseDto;
import com.nelsontr.paymentapp.dto.WebhookRequestDto;
import com.nelsontr.paymentapp.dto.WebhookResponseDto;
import com.nelsontr.paymentapp.mapper.WebhookMapper;
import com.nelsontr.paymentapp.model.Webhook;
import com.nelsontr.paymentapp.service.WebhookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/webhooks")
@Tag(name = "Webhooks", description = "Manage webhook subscriptions and receive callbacks")
public class WebhookController {
    private static final Logger log = LoggerFactory.getLogger(WebhookController.class);

    private final WebhookService webhookService;

    public WebhookController(WebhookService webhookService) {
        this.webhookService = webhookService;
    }

    @PostMapping
    @Operation(
            summary = "Register a webhook URL",
            description = "Registers a new webhook target URL that will receive payment update callbacks.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = WebhookRequestDto.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Sample request",
                                            value = "{\n  \"url\": \"http://localhost:8080/webhooks/receiver\"\n}")
                            }
                    )
            )
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Webhook registered successfully",
                    headers = @Header(name = "Location", description = "URI of the created webhook resource"),
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = WebhookResponseDto.class),
                            examples = @ExampleObject(
                                    name = "Created",
                                    value = "{\n  \"id\": 1,\n  \"url\": \"http://localhost:8080/webhooks/receiver\"\n}"))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation error or malformed JSON",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = org.springframework.http.ProblemDetail.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Blank url",
                                            value = "{\n  \"type\": \"urn:paymentapp:problem:validation-error\",\n  \"title\": \"Bad Request\",\n  \"status\": 400,\n  \"detail\": \"Validation failed for request body\",\n  \"errors\": { \n    \"url\": [\"must not be blank\"]\n  }\n}"),
                                    @ExampleObject(
                                            name = "Malformed JSON",
                                            value = "{\n  \"type\": \"urn:paymentapp:problem:malformed-json\",\n  \"title\": \"Bad Request\",\n  \"status\": 400,\n  \"detail\": \"Malformed JSON request\"\n}")
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Webhook with the given URL already exists",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = org.springframework.http.ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "Conflict",
                                    value = "{\n  \"type\": \"urn:paymentapp:problem:resource-conflict\",\n  \"title\": \"Conflict\",\n  \"status\": 409,\n  \"detail\": \"Webhook with the given URL already exists\"\n}"))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Unexpected server error",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = org.springframework.http.ProblemDetail.class)
                    )
            )
    })
    public ResponseEntity<WebhookResponseDto> registerWebhook(@Valid @RequestBody WebhookRequestDto request) {
        Webhook webhook = webhookService.registerWebhook(request.getUrl());
        return ResponseEntity.created(URI.create("/webhooks/" + webhook.getId()))
                .body(WebhookMapper.mapToDto(webhook));
    }

    // Local loopback webhook receiver for testing webhook callbacks from this app
    @PostMapping("/receiver")
    @Operation(
            summary = "Receive a webhook callback (loopback for local testing)",
            description = "This endpoint can be used as a local webhook receiver during development. It accepts the same payload sent by the webhook notifications (PaymentResponse).",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PaymentResponseDto.class),
                            examples = @ExampleObject(
                                    name = "Sample callback",
                                    value = "{\n  \"transactionId\": 1001,\n  \"orderNumber\": \"ORD-2025-000123\",\n  \"status\": \"PENDING\"\n}")
                    )
            )
    )
    @ApiResponse(responseCode = "200", description = "Callback received")
    public ResponseEntity<Void> receiveLocalWebhook(@RequestBody PaymentResponseDto payload) {
        log.info("Received webhook callback: transactionId={} orderNumber={} status={}",
                payload.getTransactionId(), payload.getOrderNumber(), payload.getStatus());
        return ResponseEntity.ok().build();
    }

}
