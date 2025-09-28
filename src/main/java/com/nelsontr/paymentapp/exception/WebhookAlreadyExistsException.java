package com.nelsontr.paymentapp.exception;

public class WebhookAlreadyExistsException extends RuntimeException {
    public WebhookAlreadyExistsException() {
        super();
    }

    public WebhookAlreadyExistsException(String message) {
        super(message);
    }

    public WebhookAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }
}

