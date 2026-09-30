package com.example.paymentform;

public final class PaymentFormServiceTest {
    public static void main(String[] args) {
        PaymentFormService service = new PaymentFormService();
        PaymentEvent event = new PaymentEvent("pay-test", 10000, "USD", "Test Customer");
        RiskDecision decision = service.decide(event);
        AuditNotification notification = service.notify(event, decision);
        if (decision != RiskDecision.REVIEW_REQUIRED) throw new AssertionError("threshold must require review");
        if (!notification.message().contains("REVIEW_REQUIRED")) throw new AssertionError("audit action missing");
        System.out.println("PASS: review decision and audit notification");
    }
}
