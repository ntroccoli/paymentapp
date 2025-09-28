package com.nelsontr.paymentapp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nelsontr.paymentapp.dto.WebhookRequestDto;
import com.nelsontr.paymentapp.exception.GlobalExceptionHandler;
import com.nelsontr.paymentapp.exception.WebhookAlreadyExistsException;
import com.nelsontr.paymentapp.model.Webhook;
import com.nelsontr.paymentapp.service.WebhookService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class WebhookControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private WebhookService webhookService;

    @BeforeEach
    void setup() {
        WebhookController controller = new WebhookController(webhookService);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    private WebhookRequestDto request(String url) {
        WebhookRequestDto dto = new WebhookRequestDto();
        dto.setUrl(url);
        return dto;
    }

    private Webhook webhook(long id, String url) {
        Webhook w = new Webhook();
        w.setId(id);
        w.setUrl(url);
        return w;
    }

    @Test
    @DisplayName("POST /webhooks returns 201 Created with Location and body when registered")
    void registerWebhook_created201() throws Exception {
        when(webhookService.registerWebhook(anyString())).thenReturn(webhook(1L, "http://localhost:9999/ok"));

        mockMvc.perform(post("/webhooks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request("http://localhost:9999/ok"))))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/webhooks/1"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.url").value("http://localhost:9999/ok"));
    }

    @Test
    @DisplayName("POST /webhooks returns 400 on validation error (blank url)")
    void registerWebhook_validationError400() throws Exception {
        mockMvc.perform(post("/webhooks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request(""))))
                .andExpect(status().isBadRequest())
                // standalone setup doesn't automatically set problem+json content-type
                .andExpect(jsonPath("$.type", containsString("validation-error")))
                .andExpect(jsonPath("$.title").value("Bad Request"));
    }

    @Test
    @DisplayName("POST /webhooks returns 409 when URL already exists")
    void registerWebhook_conflict409() throws Exception {
        when(webhookService.registerWebhook(anyString())).thenThrow(new WebhookAlreadyExistsException("Webhook with the given URL already exists"));

        mockMvc.perform(post("/webhooks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request("http://dup"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type", containsString("resource-conflict")))
                .andExpect(jsonPath("$.title").value("Conflict"));
    }

    @Test
    @DisplayName("POST /webhooks/receiver returns 200 OK")
    void receiveLocalWebhook_ok() throws Exception {
        // Build the same payload type that notifyWebhooks sends (PaymentResponseDto)
        String payload = "{\n  \"transactionId\": 42,\n  \"orderNumber\": \"ORD-TEST-0001\",\n  \"status\": \"PENDING\"\n}";

        mockMvc.perform(post("/webhooks/receiver")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());
    }
}
