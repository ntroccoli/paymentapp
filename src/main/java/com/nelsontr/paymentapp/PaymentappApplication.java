package com.nelsontr.paymentapp;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@OpenAPIDefinition(
        info = @Info(
                title = "Payment API",
                version = "v1",
                description = "Create and query payment transactions",
                contact = @Contact(name = "Support", email = "support@example.com")
        )
)
public class PaymentappApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentappApplication.class, args);
    }

}
