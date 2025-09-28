package com.nelsontr.paymentapp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nelsontr.paymentapp.dto.PaymentRequestDto;
import com.nelsontr.paymentapp.exception.GlobalExceptionHandler;
import com.nelsontr.paymentapp.model.Payment;
import com.nelsontr.paymentapp.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private PaymentService paymentService;

    @BeforeEach
    void setup() {
        PaymentController controller = new PaymentController(paymentService);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    private PaymentRequestDto validRequest() {
        PaymentRequestDto dto = new PaymentRequestDto();
        dto.setOrderNumber("ORD-2025-000123");
        dto.setFirstName("Jane");
        dto.setLastName("Doe");
        dto.setZipCode("90210");
        dto.setCardNumber("4111111111111111");
        return dto;
    }

    private Payment payment(Long id, String orderNumber) {
        Payment p = new Payment();
        p.setTransactionId(id);
        p.setOrderNumber(orderNumber);
        p.setStatus("PENDING");
        return p;
    }

    @Test
    @DisplayName("POST /payments returns 201 Created with Location when new payment is created")
    void createPayment_created201() throws Exception {
        Payment p = payment(1001L, "ORD-2025-000123");
        when(paymentService.createOrGetPayment(any(PaymentRequestDto.class)))
                .thenReturn(new PaymentService.CreatePaymentResult(p, true));

        mockMvc.perform(post("/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/payments/1001"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.transactionId").value(1001))
                .andExpect(jsonPath("$.orderNumber").value("ORD-2025-000123"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.encryptedCardNumber").doesNotExist());
    }

    @Test
    @DisplayName("POST /payments returns 200 OK when idempotent existing payment is returned")
    void createPayment_existing200() throws Exception {
        Payment p = payment(1001L, "ORD-2025-000123");
        when(paymentService.createOrGetPayment(any(PaymentRequestDto.class)))
                .thenReturn(new PaymentService.CreatePaymentResult(p, false));

        mockMvc.perform(post("/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.transactionId").value(1001));
    }

    @Test
    @DisplayName("POST /payments returns 400 with validation problem details when invalid request")
    void createPayment_validationError400() throws Exception {
        PaymentRequestDto bad = validRequest();
        bad.setZipCode("123"); // too short

        mockMvc.perform(post("/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bad)))
                .andExpect(status().isBadRequest())
                // do not assert content-type strictly in standalone setup
                .andExpect(jsonPath("$.type", containsString("validation-error")))
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.errors.zipCode", notNullValue()));
    }

    @Test
    @DisplayName("GET /payments?orderNumber=... returns 200 when found and 404 when missing")
    void getPaymentByOrderNumber_foundAndNotFound() throws Exception {
        when(paymentService.getPaymentByOrderNumber("ORD-FOUND"))
                .thenReturn(Optional.of(payment(111L, "ORD-FOUND")));
        when(paymentService.getPaymentByOrderNumber("ORD-MISSING"))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/payments").param("orderNumber", "ORD-FOUND"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value(111));

        mockMvc.perform(get("/payments").param("orderNumber", "ORD-MISSING"))
                .andExpect(status().isNotFound())
                // do not assert content-type strictly in standalone setup
                .andExpect(jsonPath("$.type", containsString("resource-not-found")))
                .andExpect(jsonPath("$.title").value("Not Found"));
    }

    @Test
    @DisplayName("GET /payments/{id} returns 200 when found")
    void getPaymentByTransactionId_found() throws Exception {
        when(paymentService.getPaymentByTransactionId(222L))
                .thenReturn(Optional.of(payment(222L, "ORD-222")));

        mockMvc.perform(get("/payments/222"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value(222))
                .andExpect(jsonPath("$.orderNumber").value("ORD-222"));
    }
}
