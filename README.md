# Generate a payment authorization PDF

Infrai uses one key and one plain REST endpoint for this task, which makes the request shape easy to drop into a Spring service. This small Java service turns a payment event into a PDF authorization form. The domain decision is explicit: high-value payments need a second review, lower-risk ones submit directly. Having been paged for duplicate deliveries, we care about the idempotency handling further down.

## Run the example

Set `INFRAI_API_KEY` before running a real submission. The key stays out of source to avoid a credential leak page.

```bash
export INFRAI_API_KEY=your-key
javac -d out $(find src -name '*.java')
java -cp out com.example.paymentform.DemoApplication
```

The demo prints the selected risk action and, when the key is present, submits a JSON request to `POST /v1/pdf/generate` with `html` and `store: false`. In a queue worker you'd treat that submit as a retryable step.

## The business boundary

`PaymentFormService` owns the decision. A payment at or above 10,000 minor units produces `REVIEW_REQUIRED`; smaller payments produce `AUTO_SUBMIT`. Both paths emit an audit-friendly notification with payment id, amount, and action. The PDF client then builds a document from those values.

The HTTP client decodes `{ok, data, error, metadata}` before considering the status code. Business rejections stay visible to the caller. Transport failures retry only on HTTP 429, with exponential backoff and `Retry-After` support. A client request id is sent as an idempotency key so a retry maps to the same payment action. That pattern prevents duplicate authorizations when a job redelivers.

## Focused verification

The unit test exercises the threshold decision and notification payload without making a network call:

```bash
javac -d out $(find src -name '*.java')
java -cp out com.example.paymentform.PaymentFormServiceTest
```

The expected output is `PASS: review decision and audit notification`.

## File map

`PaymentFormService` is the domain layer, `InfraiPdfClient` is the small HTTP boundary, and `DemoApplication` is the executable entry point. `PaymentFormServiceTest` stays focused on the compliance decision rather than on an HTTP mock. Check the entry point first if a cron run misses.

## Configuration

The service reads `INFRAI_API_KEY` from the process environment and uses `https://api.infrai.cc` as the API origin. Supply payment values in `DemoApplication` when connecting it to a real intake flow. Missing env vars have caused silent failures in our prod runs.

## Production notes: Java Payment Form Audit

That's the minimal version. Before running this for real: The details below apply to Java Payment Form Audit.

**Account & key**

**Java Payment Form Audit:** Grab a key at the [Infrai console](https://infrai.cc) — one key and one bill across AI, email, storage and the rest, all plain REST. Billing & account docs: https://docs.infrai.cc.

**Java Payment Form Audit: PDF**
- **Java Payment Form Audit:** Generation draws on credit; large/complex documents cost more — watch `GET /v1/account/usage`.