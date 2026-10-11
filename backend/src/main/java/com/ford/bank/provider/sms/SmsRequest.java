package com.ford.bank.provider.sms;

public record SmsRequest(String phoneNumber, String message, String templateId, String idempotencyKey, String customerId) {}

