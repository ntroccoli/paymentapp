package com.nelsontr.paymentapp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nelsontr.paymentapp.dto.PaymentRequestDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class PaymentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private PaymentRequestDto validRequest() {
        PaymentRequestDto dto = new PaymentRequestDto();
        dto.setOrderNumber("ORD-IT-0001");
        dto.setFirstName("Alice");
        dto.setLastName("Wonder");
        dto.setZipCode("10001");
        dto.setCardNumber("4111111111111111");
        return dto;
    }

    @Test
    void endToEnd_create_thenGet_thenIdempotent() throws Exception {
        // Create
        MvcResult created = mockMvc.perform(post("/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/payments/")))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        JsonNode node = objectMapper.readTree(created.getResponse().getContentAsString());
        long txId = node.get("transactionId").asLong();
        assertThat(txId).isPositive();

        // GET by orderNumber
        mockMvc.perform(get("/payments").param("orderNumber", "ORD-IT-0001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value(txId))
                .andExpect(jsonPath("$.orderNumber").value("ORD-IT-0001"))
                .andExpect(jsonPath("$.status").value("PENDING"));

        // GET by transactionId
        mockMvc.perform(get("/payments/" + txId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value(txId));

        // Create same order again -> idempotent 200 OK with same txId
        MvcResult existing = mockMvc.perform(post("/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();
        long txId2 = objectMapper.readTree(existing.getResponse().getContentAsString()).get("transactionId").asLong();
        assertThat(txId2).isEqualTo(txId);
    }

    @Test
    void errorFlows_validation_and_notFound() throws Exception {
        // Validation error
        PaymentRequestDto bad = validRequest();
        bad.setZipCode("999");

        mockMvc.perform(post("/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bad)))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Content-Type", containsString("application/problem+json")))
                .andExpect(jsonPath("$.type", containsString("validation-error")))
                .andExpect(jsonPath("$.title").value("Bad Request"));

        // Not found
        mockMvc.perform(get("/payments").param("orderNumber", "ORD-NOT-EXIST"))
                .andExpect(status().isNotFound())
                .andExpect(header().string("Content-Type", containsString("application/problem+json")))
                .andExpect(jsonPath("$.type", containsString("resource-not-found")))
                .andExpect(jsonPath("$.title").value("Not Found"));
    }
}

