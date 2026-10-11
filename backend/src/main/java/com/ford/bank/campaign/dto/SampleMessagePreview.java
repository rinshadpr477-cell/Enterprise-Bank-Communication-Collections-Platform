package com.ford.bank.campaign.dto;

import java.math.BigDecimal;

public record SampleMessagePreview(
        String accountNumber,
        String customerName,
        String phoneNumber,
        String channel,
        String renderedMessage,
        BigDecimal overdueAmount,
        String priority
) {}
