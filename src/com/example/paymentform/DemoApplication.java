package com.example.paymentform;

import java.net.http.HttpClient;

public final class DemoApplication {
    public static void main(String[] args) throws Exception {
        PaymentEvent event = new PaymentEvent("pay-1042", 12500, "USD", "A. Chen");
        PaymentFormService service = new PaymentFormService();
        RiskDecision decision = service.decide(event);
        System.out.println(service.notify(event, decision));
        String key = System.getenv("INFRAI_API_KEY");
        if (key == null || key.isBlank()) {
            System.out.println("Set INFRAI_API_KEY to generate the payment PDF.");
            return;
        }
        String result = new InfraiPdfClient(HttpClient.newHttpClient(), key)
                .generate(event.paymentId(), event.customerName(), event.amountMinor(), event.currency());
        System.out.println(result);
    }
}
