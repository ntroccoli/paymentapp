package com.nelsontr.paymentapp.repository;

import com.nelsontr.paymentapp.model.Payment;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class PaymentRepositoryTest {

    @Autowired
    private PaymentRepository paymentRepository;

    @Test
    void findByOrderNumber_returnsSavedEntity() {
        Payment p = new Payment();
        p.setOrderNumber("ORD-ABC-1");
        p.setFirstName("John");
        p.setLastName("Smith");
        p.setZipCode("12345");
        p.setEncryptedCardNumber("ENC");
        p.setStatus("PENDING");
        paymentRepository.save(p);

        Optional<Payment> fetched = paymentRepository.findByOrderNumber("ORD-ABC-1");
        assertThat(fetched).isPresent();
        assertThat(fetched.get().getTransactionId()).isNotNull();
        assertThat(fetched.get().getOrderNumber()).isEqualTo("ORD-ABC-1");
    }

    @Test
    void findByOrderNumber_returnsEmptyWhenMissing() {
        assertThat(paymentRepository.findByOrderNumber("NOPE")).isEmpty();
    }
}

