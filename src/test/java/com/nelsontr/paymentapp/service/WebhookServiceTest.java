package com.nelsontr.paymentapp.service;

import com.nelsontr.paymentapp.model.Payment;
import com.nelsontr.paymentapp.model.Webhook;
import com.nelsontr.paymentapp.repository.WebhookRepository;
import com.nelsontr.paymentapp.exception.WebhookAlreadyExistsException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class WebhookServiceTest {

    private WebhookRepository webhookRepository;
    private WebhookService webhookService;

    private HttpServer server;
    private int port;

    @BeforeEach
    void setUp() throws Exception {
        webhookRepository = Mockito.mock(WebhookRepository.class);
        webhookService = new WebhookService(webhookRepository);

        server = HttpServer.create(new InetSocketAddress(0), 0); // random free port
        port = server.getAddress().getPort();
        server.start();
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    private Payment payment(long id, String orderNumber) {
        Payment p = new Payment();
        p.setTransactionId(id);
        p.setOrderNumber(orderNumber);
        p.setStatus("PENDING");
        return p;
    }

    private Webhook webhook(String url) {
        Webhook w = new Webhook();
        w.setId(1L);
        w.setUrl(url);
        return w;
    }

    @Test
    void registerWebhook_throwsWhenExists() {
        when(webhookRepository.findByUrl("http://dup")).thenReturn(Optional.of(new Webhook()));
        assertThrows(WebhookAlreadyExistsException.class, () -> webhookService.registerWebhook("http://dup"));
        verify(webhookRepository, never()).save(any());
    }

    @Test
    void registerWebhook_savesWhenNew() {
        when(webhookRepository.findByUrl("http://new")).thenReturn(Optional.empty());
        ArgumentCaptor<Webhook> captor = ArgumentCaptor.forClass(Webhook.class);
        Webhook saved = new Webhook();
        saved.setId(42L);
        saved.setUrl("http://new");
        when(webhookRepository.save(any())).thenReturn(saved);

        Webhook out = webhookService.registerWebhook("http://new");
        verify(webhookRepository).save(captor.capture());
        assertThat(captor.getValue().getUrl()).isEqualTo("http://new");
        assertThat(out.getId()).isEqualTo(42L);
    }

    @Test
    void notifyWebhooks_nullPayment_noRepoCall() {
        webhookService.notifyWebhooks(null);
        verifyNoInteractions(webhookRepository);
    }

    @Test
    void notifyWebhooks_noRegisteredWebhooks() {
        when(webhookRepository.findAll()).thenReturn(new ArrayList<>());
        webhookService.notifyWebhooks(payment(1L, "ORD-UNIT-0"));
        verify(webhookRepository).findAll();
    }

    @Test
    void notifyWebhooks_postsJsonPayload_success() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        final String path = "/ok";
        final AtomicInteger calls = new AtomicInteger();

        server.createContext(path, exchange -> {
            calls.incrementAndGet();
            assertThat(exchange.getRequestMethod()).isEqualTo("POST");
            assertThat(exchange.getRequestHeaders().getFirst("Content-Type")).contains("application/json");
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            // ensure payload contains safe fields only
            assertThat(body).contains("\"transactionId\":1");
            assertThat(body).contains("\"orderNumber\":\"ORD-UNIT-1\"");
            assertThat(body).contains("\"status\":\"PENDING\"");
            // respond 200
            byte[] resp = "ok".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, resp.length);
            try (OutputStream os = exchange.getResponseBody()) { os.write(resp); }
            latch.countDown();
        });

        when(webhookRepository.findAll()).thenReturn(List.of(webhook("http://localhost:" + port + path)));

        webhookService.notifyWebhooks(payment(1L, "ORD-UNIT-1"));

        assertThat(latch.await(3, TimeUnit.SECONDS)).as("webhook delivered").isTrue();
        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    void notifyWebhooks_retriesOnFailure_thenSuccessOnThirdAttempt() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        final String path = "/flaky";
        final AtomicInteger calls = new AtomicInteger();

        server.createContext(path, new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                int n = calls.incrementAndGet();
                if (n < 3) {
                    // fail first two attempts
                    exchange.sendResponseHeaders(500, -1);
                    exchange.close();
                } else {
                    // succeed on 3rd
                    byte[] resp = "ok".getBytes(StandardCharsets.UTF_8);
                    exchange.sendResponseHeaders(200, resp.length);
                    try (OutputStream os = exchange.getResponseBody()) { os.write(resp); }
                    latch.countDown();
                }
            }
        });

        when(webhookRepository.findAll()).thenReturn(List.of(webhook("http://localhost:" + port + path)));

        webhookService.notifyWebhooks(payment(2L, "ORD-UNIT-2"));

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(calls.get()).isEqualTo(3);
    }
}

