package com.nelsontr.paymentapp.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CardEncryptionServiceTest {

    @Test
    void encryptThenDecrypt_roundTrips() {
        CardEncryptionService svc = new CardEncryptionService("secret-password", "beefbeef");
        String original = "4111111111111111";
        String encrypted = svc.encryptCardNumber(original);
        assertThat(encrypted).isNotBlank().isNotEqualTo(original);
        String decrypted = svc.decryptCardNumber(encrypted);
        assertThat(decrypted).isEqualTo(original);
    }

    @Test
    void encryptingTwice_producesDifferentCiphertexts() {
        CardEncryptionService svc = new CardEncryptionService("secret-password", "beefbeef");
        String original = "4242424242424242";
        String c1 = svc.encryptCardNumber(original);
        String c2 = svc.encryptCardNumber(original);
        assertThat(c1).isNotEqualTo(c2);
        // both should decrypt to the same original
        assertThat(svc.decryptCardNumber(c1)).isEqualTo(original);
        assertThat(svc.decryptCardNumber(c2)).isEqualTo(original);
    }
}

