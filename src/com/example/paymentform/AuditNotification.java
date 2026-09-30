package com.example.paymentform;

public record AuditNotification(String paymentId, long amountMinor, String action, String message) {}
