package com.nelsontr.paymentapp.mapper;

import com.nelsontr.paymentapp.dto.PaymentRequestDto;
import com.nelsontr.paymentapp.dto.PaymentResponseDto;
import com.nelsontr.paymentapp.model.Payment;

public class PaymentMapper {
    public static PaymentResponseDto mapToDto(Payment payment) {
        PaymentResponseDto dto = new PaymentResponseDto();
        dto.setTransactionId(payment.getTransactionId());
        dto.setOrderNumber(payment.getOrderNumber());
        dto.setStatus(payment.getStatus());
        return dto;
    }

    public static Payment mapToEntity(PaymentRequestDto request, String encryptedCardNumber) {
        Payment payment = new Payment();
        payment.setOrderNumber(request.getOrderNumber());
        payment.setFirstName(request.getFirstName());
        payment.setLastName(request.getLastName());
        payment.setZipCode(request.getZipCode());
        payment.setEncryptedCardNumber(encryptedCardNumber); // Assume encryption is handled elsewhere
        payment.setStatus("PENDING");
        return payment;
    }
}
