package com.nelsontr.paymentapp.service;

import com.nelsontr.paymentapp.dto.PaymentResponseDto;
import com.nelsontr.paymentapp.mapper.PaymentMapper;
import com.nelsontr.paymentapp.model.Payment;
import com.nelsontr.paymentapp.model.Webhook;
import com.nelsontr.paymentapp.repository.WebhookRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import com.nelsontr.paymentapp.exception.WebhookAlreadyExistsException;

@Service
public class WebhookService {
    private static final Logger log = LoggerFactory.getLogger(WebhookService.class);

    private final WebhookRepository webhookRepository;

    public WebhookService(WebhookRepository webhookRepository) {
        this.webhookRepository = webhookRepository;
    }

    @Async
    public void notifyWebhooks(Payment payment) {
        if (payment == null) {
            log.warn("notifyWebhooks called with null payment; skipping");
            return;
        }
        List<Webhook> webhooks = webhookRepository.findAll();
        if (webhooks.isEmpty()) {
            log.info("No webhooks registered; skipping notification for payment transactionId={}", payment.getTransactionId());
            return;
        }

        // Build a safe payload (no PII / sensitive fields)
        PaymentResponseDto payload = PaymentMapper.mapToDto(payment);

        // Create a RestTemplate with modest timeouts
        SimpleClientHttpRequestFactory reqFactory = new SimpleClientHttpRequestFactory();
        reqFactory.setConnectTimeout((int) Duration.ofSeconds(2).toMillis());
        reqFactory.setReadTimeout((int) Duration.ofSeconds(3).toMillis());
        RestTemplate restTemplate = new RestTemplate(reqFactory);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<PaymentResponseDto> requestEntity = new HttpEntity<>(payload, headers);

        final int maxAttempts = 3;
        for (Webhook hook : webhooks) {
            String url = hook.getUrl();
            long backoffMs = 200; // simple exponential backoff
            for (int attempt = 1; attempt <= maxAttempts; attempt++) {
                try {
                    log.info("Webhook call attempt {}/{} -> url={} paymentId={} orderNumber={}", attempt, maxAttempts, url, payload.getTransactionId(), payload.getOrderNumber());
                    ResponseEntity<String> response = restTemplate.postForEntity(url, requestEntity, String.class);
                    int status = response.getStatusCode().value();
                    if (status >= 200 && status < 300) {
                        log.info("Webhook success url={} status={}", url, status);
                        break; // success, stop retrying this URL
                    } else {
                        log.warn("Webhook non-2xx response url={} status={}", url, status);
                    }
                } catch (RestClientResponseException e) {
                    // An HTTP response was received but not 2xx
                    log.warn("Webhook HTTP error url={} status={} body={} attempt={}", url, e.getStatusCode().value(), e.getResponseBodyAsString(), attempt);
                } catch (Exception e) {
                    log.warn("Webhook call failed url={} attempt={} error={}", url, attempt, e.toString());
                }

                // Backoff before next attempt (unless this was the last one)
                if (attempt < maxAttempts) {
                    try {
                        Thread.sleep(backoffMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.warn("Retry sleep interrupted for url={}", url);
                        break; // stop retrying this URL
                    }
                    backoffMs = Math.min(backoffMs * 2, 2000); // cap backoff
                }
            }
        }
    }

    public Webhook registerWebhook(String url) {
        Optional<Webhook> webhook = webhookRepository.findByUrl(url);
        if (webhook.isPresent()) {
            throw new WebhookAlreadyExistsException("Webhook with the given URL already exists");
        }
        Webhook newWebhook = new Webhook();
        newWebhook.setUrl(url);
        return webhookRepository.save(newWebhook);
    }
}
