package com.nelsontr.paymentapp.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Component;

@Component
public class CardEncryptionService {

    private final TextEncryptor textEncryptor;

    public CardEncryptionService(@Value("${app.encryption.key}") String key,
                                 @Value("${app.encryption.salt}") String salt) {
        this.textEncryptor = Encryptors.text(key, salt);
    }

    public String encryptCardNumber(String cardNumber) {
        return textEncryptor.encrypt(cardNumber);
    }

    public String decryptCardNumber(String encryptedCard) {
        return textEncryptor.decrypt(encryptedCard);
    }
}
