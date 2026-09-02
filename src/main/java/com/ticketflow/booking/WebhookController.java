package com.ticketflow.booking;

import com.ticketflow.booking.dto.PaymentWebhookRequest;
import com.ticketflow.booking.dto.WebhookAck;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * POST /webhooks/payment — the (fake) payment provider calls this. It is public
 * (allow-listed in SecurityConfig); a real provider would sign its requests and
 * we would verify the signature here instead of using our JWT.
 *
 * We answer 200 for anything we successfully received and reasoned about —
 * including "duplicate", "unknown booking", and "hold expired". Returning an
 * error would make the provider retry forever. The only non-200 here is a
 * malformed body (400 from @Valid), which a retry cannot fix anyway.
 */
@RestController
@RequestMapping("/webhooks")
@RequiredArgsConstructor
public class WebhookController {

    private final PaymentWebhookService paymentWebhookService;

    @PostMapping("/payment")
    public WebhookAck payment(@Valid @RequestBody PaymentWebhookRequest request) {
        return new WebhookAck(paymentWebhookService.handle(request));
    }
}
