package com.nelsontr.paymentapp.service;

import com.nelsontr.paymentapp.dto.PaymentRequestDto;
import com.nelsontr.paymentapp.model.Payment;
import com.nelsontr.paymentapp.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private CardEncryptionService cardEncryptionService;

    @Mock
    private WebhookService webhookService;

    @InjectMocks
    private PaymentService paymentService;

    private PaymentRequestDto request;

    @BeforeEach
    void setUp() {
        request = new PaymentRequestDto();
        request.setOrderNumber("ORD-2025-000123");
        request.setFirstName("Jane");
        request.setLastName("Doe");
        request.setZipCode("90210");
        request.setCardNumber("4111111111111111");
    }

    @Test
    void createOrGetPayment_returnsExisting_whenOrderNumberAlreadyExists() {
        Payment existing = new Payment();
        existing.setTransactionId(1001L);
        existing.setOrderNumber(request.getOrderNumber());
        existing.setStatus("PENDING");
        when(paymentRepository.findByOrderNumber(request.getOrderNumber())).thenReturn(Optional.of(existing));

        PaymentService.CreatePaymentResult result = paymentService.createOrGetPayment(request);

        assertThat(result.isCreated()).isFalse();
        assertThat(result.getPayment()).isSameAs(existing);
        verify(paymentRepository, never()).save(any());
        verifyNoInteractions(cardEncryptionService);
        verifyNoInteractions(webhookService);
    }

    @Test
    void createOrGetPayment_createsNew_whenOrderNumberIsNew() {
        when(paymentRepository.findByOrderNumber(request.getOrderNumber())).thenReturn(Optional.empty());
        when(cardEncryptionService.encryptCardNumber("4111111111111111")).thenReturn("ENC-4111");

        ArgumentCaptor<Payment> toSaveCaptor = ArgumentCaptor.forClass(Payment.class);
        Payment saved = new Payment();
        saved.setTransactionId(2002L);
        saved.setOrderNumber(request.getOrderNumber());
        saved.setStatus("PENDING");
        when(paymentRepository.save(toSaveCaptor.capture())).thenReturn(saved);

        PaymentService.CreatePaymentResult result = paymentService.createOrGetPayment(request);

        assertThat(result.isCreated()).isTrue();
        assertThat(result.getPayment().getTransactionId()).isEqualTo(2002L);
        // Verify mapping and encryption
        Payment toSave = toSaveCaptor.getValue();
        assertThat(toSave.getOrderNumber()).isEqualTo(request.getOrderNumber());
        assertThat(toSave.getFirstName()).isEqualTo(request.getFirstName());
        assertThat(toSave.getLastName()).isEqualTo(request.getLastName());
        assertThat(toSave.getZipCode()).isEqualTo(request.getZipCode());
        assertThat(toSave.getEncryptedCardNumber()).isEqualTo("ENC-4111");
        assertThat(toSave.getStatus()).isEqualTo("PENDING");

        verify(cardEncryptionService).encryptCardNumber("4111111111111111");
        verify(paymentRepository).save(any(Payment.class));
        verify(webhookService).notifyWebhooks(saved);
    }

    @Test
    void getPaymentByOrderNumber_delegatesToRepository() {
        when(paymentRepository.findByOrderNumber("ORD-1")).thenReturn(Optional.empty());
        assertThat(paymentService.getPaymentByOrderNumber("ORD-1")).isEmpty();
        verify(paymentRepository).findByOrderNumber("ORD-1");
    }

    @Test
    void getPaymentByTransactionId_delegatesToRepository() {
        when(paymentRepository.findById(42L)).thenReturn(Optional.empty());
        assertThat(paymentService.getPaymentByTransactionId(42L)).isEmpty();
        verify(paymentRepository).findById(42L);
    }
}
