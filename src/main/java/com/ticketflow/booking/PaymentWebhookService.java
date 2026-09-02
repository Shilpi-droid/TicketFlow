package com.ticketflow.booking;

import com.ticketflow.booking.dto.PaymentWebhookRequest;
import com.ticketflow.repository.WebhookEventRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/**
 * Handles one delivery of a payment webhook.
 *
 * The order is deliberate (CLAUDE.md §6):
 *   1. Write the event to webhook_events FIRST (via ON CONFLICT DO NOTHING).
 *   2. If it was a duplicate provider_event_id, stop — we already handled it.
 *   3. Otherwise act on it (confirm / cancel the booking).
 *   4. Stamp processed_at.
 *
 * Everything runs in ONE transaction, so the webhook_events row and the booking
 * change commit together. If step 3 throws an unexpected error, the whole thing
 * rolls back (including the webhook_events row) and the provider's retry gets a
 * fresh attempt — which is what we want for a genuine failure.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentWebhookService {

    private final WebhookEventRepository webhookEventRepository;
    private final BookingService bookingService;
    private final ObjectMapper objectMapper;

    @Transactional
    public String handle(PaymentWebhookRequest request) {
        String json = toJson(request);

        int inserted = webhookEventRepository.insertIfNew(request.providerEventId(), json);
        if (inserted == 0) {
            log.info("Webhook {} already processed — ignoring", request.providerEventId());
            return "duplicate_ignored";
        }

        BookingService.ConfirmOutcome outcome = switch (request.type()) {
            case PaymentWebhookRequest.PAYMENT_SUCCEEDED -> bookingService.confirm(request.bookingId());
            case PaymentWebhookRequest.PAYMENT_FAILED -> bookingService.cancel(request.bookingId());
            default -> {
                log.warn("Unknown webhook type '{}'", request.type());
                yield BookingService.ConfirmOutcome.NOT_PENDING;
            }
        };

        webhookEventRepository.markProcessed(request.providerEventId(), Instant.now());
        return outcome.name().toLowerCase();
    }

    private String toJson(PaymentWebhookRequest request) {
        // Jackson 3 (tools.jackson.*) throws an UNCHECKED JacksonException, so no
        // try/catch here — and a record of simple fields never fails anyway.
        return objectMapper.writeValueAsString(request);
    }
}
