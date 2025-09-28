package com.nelsontr.paymentapp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nelsontr.paymentapp.dto.PaymentRequestDto;
import com.nelsontr.paymentapp.dto.WebhookRequestDto;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class WebhookIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private HttpServer server;
    private int port;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        port = server.getAddress().getPort();
        server.start();
    }

    @AfterEach
    void tearDown() {
        if (server != null) server.stop(0);
    }

    private PaymentRequestDto validPayment() {
        PaymentRequestDto dto = new PaymentRequestDto();
        dto.setOrderNumber("ORD-WH-IT-0001");
        dto.setFirstName("Ivy");
        dto.setLastName("Tester");
        dto.setZipCode("12345");
        dto.setCardNumber("4111111111111111");
        return dto;
    }

    private WebhookRequestDto webhookReq(String url) {
        WebhookRequestDto dto = new WebhookRequestDto();
        dto.setUrl(url);
        return dto;
    }

    @Test
    void registerWebhook_and_receiveCallback_onPaymentCreate() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger calls = new AtomicInteger();
        String path = "/received";
        final StringBuilder lastBody = new StringBuilder();

        server.createContext(path, (HttpExchange exchange) -> {
            calls.incrementAndGet();
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            lastBody.setLength(0);
            lastBody.append(body);
            byte[] resp = "ok".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, resp.length);
            try (OutputStream os = exchange.getResponseBody()) { os.write(resp); }
            latch.countDown();
        });

        String hookUrl = "http://localhost:" + port + path;

        // Register webhook -> 201 Created
        mockMvc.perform(post("/webhooks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(webhookReq(hookUrl))))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/webhooks/")))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.url").value(hookUrl));

        // Create payment -> should trigger async webhook POST to our server
        mockMvc.perform(post("/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validPayment())))
                .andExpect(status().isCreated());

        // Wait for webhook delivery
        assertThat(latch.await(4, TimeUnit.SECONDS)).isTrue();
        assertThat(calls.get()).isEqualTo(1);
        assertThat(lastBody.toString()).contains("\"orderNumber\":\"ORD-WH-IT-0001\"");
        assertThat(lastBody.toString()).contains("\"status\":\"PENDING\"");
    }

    @Test
    void registerWebhook_conflict409_integration() throws Exception {
        String hookUrl = "http://localhost:" + port + "/once";

        // First time OK
        mockMvc.perform(post("/webhooks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(webhookReq(hookUrl))))
                .andExpect(status().isCreated());

        // Second time same URL -> 409 with problem+json
        mockMvc.perform(post("/webhooks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(webhookReq(hookUrl))))
                .andExpect(status().isConflict())
                .andExpect(header().string("Content-Type", containsString("application/problem+json")))
                .andExpect(jsonPath("$.type", containsString("resource-conflict")))
                .andExpect(jsonPath("$.title").value("Conflict"));
    }

    @Test
    void registerWebhook_validationError400_integration() throws Exception {
        mockMvc.perform(post("/webhooks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(webhookReq(""))))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Content-Type", containsString("application/problem+json")))
                .andExpect(jsonPath("$.type", containsString("validation-error")))
                .andExpect(jsonPath("$.title").value("Bad Request"));
    }
}

