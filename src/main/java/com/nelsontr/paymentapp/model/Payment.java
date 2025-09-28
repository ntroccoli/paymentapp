package com.nelsontr.paymentapp.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Data
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long transactionId;

    @Column(unique = true, nullable = false)
    private String orderNumber;
    private String firstName;
    private String lastName;
    private String zipCode;
    private String encryptedCardNumber;
    private String status;
}
