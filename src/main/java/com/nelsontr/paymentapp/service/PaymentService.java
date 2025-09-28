package com.nelsontr.paymentapp.service;

import com.nelsontr.paymentapp.dto.PaymentRequestDto;
import com.nelsontr.paymentapp.mapper.PaymentMapper;
import com.nelsontr.paymentapp.model.Payment;
import com.nelsontr.paymentapp.repository.PaymentRepository;
import jakarta.validation.Valid;
import lombok.Getter;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final WebhookService webhookService;
    private final CardEncryptionService cardEncryptionService;

    public PaymentService(PaymentRepository paymentRepository, WebhookService webhookService, CardEncryptionService cardEncryptionService) {
        this.paymentRepository = paymentRepository;
        this.webhookService = webhookService;
        this.cardEncryptionService = cardEncryptionService;
    }

    /**
     * Idempotent create: If a payment with the same order number exists, return it and mark created=false.
     * Otherwise, persist a new payment and mark created=true.
     */
    public CreatePaymentResult createOrGetPayment(@Valid PaymentRequestDto request) {
        String orderNumber = request.getOrderNumber();
        Optional<Payment> existing = paymentRepository.findByOrderNumber(orderNumber);
        if (existing.isPresent()) {
            return new CreatePaymentResult(existing.get(), false);
        }
        String encryptedCardNumber = cardEncryptionService.encryptCardNumber(request.getCardNumber());
        Payment payment = PaymentMapper.mapToEntity(request, encryptedCardNumber);

        Payment saved = paymentRepository.save(payment);
        webhookService.notifyWebhooks(saved);
        return new CreatePaymentResult(saved, true);
    }

    public Optional<Payment> getPaymentByOrderNumber(String orderNumber) {
        return paymentRepository.findByOrderNumber(orderNumber);
    }

    public Optional<Payment> getPaymentByTransactionId(Long transactionId) {
        return paymentRepository.findById(transactionId);
    }

    @Getter
    public static class CreatePaymentResult {
        private final Payment payment;
        private final boolean created;

        public CreatePaymentResult(Payment payment, boolean created) {
            this.payment = payment;
            this.created = created;
        }

    }

}
