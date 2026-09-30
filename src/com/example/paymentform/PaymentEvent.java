package com.example.paymentform;

public record PaymentEvent(String paymentId, long amountMinor, String currency, String customerName) {
    public PaymentEvent {
        if (paymentId == null || paymentId.isBlank()) throw new IllegalArgumentException("paymentId is required");
        if (amountMinor < 0) throw new IllegalArgumentException("amountMinor must be non-negative");
    }
}
