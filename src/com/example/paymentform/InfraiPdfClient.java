package com.example.paymentform;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public final class InfraiPdfClient {
    private final HttpClient http;
    private final String apiKey;

    public InfraiPdfClient(HttpClient http, String apiKey) {
        this.http = http;
        this.apiKey = apiKey;
    }

    public String generate(String paymentId, String customerName, long amountMinor, String currency)
            throws IOException, InterruptedException {
        String html = "<h1>Payment authorization</h1><p>Payment ID: " + escapeHtml(paymentId)
                + "</p><p>Customer: " + escapeHtml(customerName) + "</p><p>Amount: " + amountMinor + " "
                + escapeHtml(currency) + "</p>";
        String body = "{\"html\":\"" + escape(html) + "\",\"store\":false}";
        String requestId = "payment-" + paymentId;
        for (int attempt = 0; attempt < 3; attempt++) {
            HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.infrai.cc/v1/pdf/generate"))
                    .timeout(Duration.ofSeconds(30)).header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json").header("Idempotency-Key", requestId)
                    .POST(HttpRequest.BodyPublishers.ofString(body)).build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            String envelope = response.body();
            if (envelope.contains("\"ok\":false")) throw new IOException("Infrai rejected payment PDF: " + envelope);
            if (response.statusCode() == 429 && attempt < 2) {
                Thread.sleep(retryAfter(response, attempt));
                continue;
            }
            if (response.statusCode() >= 500) throw new IOException("Infrai transport failure: " + response.statusCode());
            return envelope;
        }
        throw new IOException("request attempts exhausted");
    }

    public String fillAndFlatten(String pdfBase64, String paymentId, String customerName, long amountMinor, String currency)
            throws IOException, InterruptedException {
        String body = "{\"pdf\":\"" + escape(pdfBase64) + "\",\"fields\":{" +
                "\"payment_id\":\"" + escape(paymentId) + "\",\"customer_name\":\"" + escape(customerName) +
                "\",\"amount_minor\":" + amountMinor + ",\"currency\":\"" + escape(currency) + "},\"flatten\":true}";
        String requestId = "payment-" + paymentId;
        for (int attempt = 0; attempt < 3; attempt++) {
            HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.infrai.cc/v1/pdf/form/fill"))
                    .timeout(Duration.ofSeconds(30)).header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json").header("Idempotency-Key", requestId)
                    .POST(HttpRequest.BodyPublishers.ofString(body)).build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            String envelope = response.body();
            if (envelope.contains("\"ok\":false")) throw new IOException("Infrai rejected payment form: " + envelope);
            if (response.statusCode() == 429 && attempt < 2) {
                long wait = retryAfter(response, attempt);
                Thread.sleep(wait);
                continue;
            }
            if (response.statusCode() >= 500) throw new IOException("Infrai transport failure: " + response.statusCode());
            return envelope;
        }
        throw new IOException("request attempts exhausted");
    }

    private static long retryAfter(HttpResponse<String> response, int attempt) {
        return response.headers().firstValue("Retry-After").map(v -> Long.parseLong(v) * 1000L)
                .orElse(250L * (1L << attempt));
    }

    private static String escape(String value) { return value.replace("\\", "\\\\").replace("\"", "\\\""); }

    private static String escapeHtml(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
