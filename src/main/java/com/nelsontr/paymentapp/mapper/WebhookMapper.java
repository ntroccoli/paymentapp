package com.nelsontr.paymentapp.mapper;

import com.nelsontr.paymentapp.dto.WebhookResponseDto;
import com.nelsontr.paymentapp.model.Webhook;

public class WebhookMapper {
    public static WebhookResponseDto mapToDto(Webhook webhook) {
        WebhookResponseDto dto = new WebhookResponseDto();
        dto.setId(webhook.getId());
        dto.setUrl(webhook.getUrl());
        return dto;
    }

    public static Webhook mapToEntity(WebhookResponseDto dto) {
        Webhook webhook = new Webhook();
        webhook.setId(dto.getId());
        webhook.setUrl(dto.getUrl());
        return webhook;
    }
}
