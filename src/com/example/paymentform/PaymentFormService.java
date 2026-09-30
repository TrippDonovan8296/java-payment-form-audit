package com.example.paymentform;

public final class PaymentFormService {
    private static final long REVIEW_THRESHOLD_MINOR = 10_000;

    public RiskDecision decide(PaymentEvent event) {
        return event.amountMinor() >= REVIEW_THRESHOLD_MINOR ? RiskDecision.REVIEW_REQUIRED : RiskDecision.AUTO_SUBMIT;
    }

    public AuditNotification notify(PaymentEvent event, RiskDecision decision) {
        String message = "Payment " + event.paymentId() + " for " + event.amountMinor() + " " + event.currency()
                + " is " + decision.name();
        return new AuditNotification(event.paymentId(), event.amountMinor(), decision.name(), message);
    }
}
