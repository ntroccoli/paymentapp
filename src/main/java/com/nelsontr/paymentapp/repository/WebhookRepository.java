package com.nelsontr.paymentapp.repository;

import com.nelsontr.paymentapp.model.Webhook;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WebhookRepository extends JpaRepository<Webhook, Long> {
    Optional<Webhook> findByUrl(String url);
}
