package com.ticketflow.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ticketflow.booking.dto.BookingResponse;
import com.ticketflow.booking.dto.PaymentWebhookRequest;
import com.ticketflow.domain.BookingStatus;
import com.ticketflow.domain.Seat;
import com.ticketflow.domain.User;
import com.ticketflow.hold.HoldService;
import com.ticketflow.repository.BookingRepository;
import com.ticketflow.repository.BookingSeatRepository;
import com.ticketflow.repository.SeatHoldRepository;
import com.ticketflow.repository.SeatRepository;
import com.ticketflow.repository.UserRepository;
import com.ticketflow.repository.WebhookEventRepository;
import com.ticketflow.support.AbstractIntegrationTest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Phase 6 — checkout and idempotent payment webhooks.
 */
@AutoConfigureMockMvc
class CheckoutWebhookTest extends AbstractIntegrationTest {

    private static final long EVENT_ID = 1L;

    @Autowired private HoldService holdService;
    @Autowired private CheckoutService checkoutService;
    @Autowired private PaymentWebhookService paymentWebhookService;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private BookingSeatRepository bookingSeatRepository;
    @Autowired private SeatHoldRepository seatHoldRepository;
    @Autowired private SeatRepository seatRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private WebhookEventRepository webhookEventRepository;
    @Autowired private MockMvc mockMvc;

    // The JWT filter and MockMvc are exercised in the HTTP tests; token is real.
    @Autowired private com.ticketflow.auth.JwtService jwtService;

    private Long userId;
    private List<Long> seatIds;

    @BeforeEach
    void setUp() {
        bookingSeatRepository.deleteAllInBatch();
        bookingRepository.deleteAllInBatch();
        seatHoldRepository.deleteAllInBatch();
        webhookEventRepository.deleteAllInBatch();

        User user = new User();
        user.setEmail("buyer-" + UUID.randomUUID() + "@test.local");
        user.setPasswordHash("x");
        userId = userRepository.save(user).getId();

        seatIds = seatRepository.findByEventIdOrderById(EVENT_ID).stream().map(Seat::getId).toList();
    }

    // ---- checkout -------------------------------------------------------------

    @Test
    void checkout_creates_a_pending_booking_priced_from_the_seats() {
        UUID holdGroup = holdService.hold(EVENT_ID, userId, List.of(seatIds.get(0), seatIds.get(1))).holdGroupId();

        BookingResponse booking = checkoutService.checkout(userId, holdGroup, key());

        assertThat(booking.status()).isEqualTo(BookingStatus.PENDING);
        assertThat(booking.seatIds()).containsExactly(seatIds.get(0), seatIds.get(1));
        assertThat(booking.totalCents()).isEqualTo(12000L + 12000L);   // two Orchestra seats
        assertThat(bookingRepository.count()).isEqualTo(1);
    }

    @Test
    void checkout_with_the_same_idempotency_key_returns_the_same_booking() {
        UUID holdGroup = holdService.hold(EVENT_ID, userId, List.of(seatIds.get(2))).holdGroupId();
        String key = key();

        BookingResponse first = checkoutService.checkout(userId, holdGroup, key);
        BookingResponse second = checkoutService.checkout(userId, holdGroup, key);

        assertThat(second.id()).isEqualTo(first.id());
        assertThat(bookingRepository.count()).isEqualTo(1);
    }

    // ---- webhook: the idempotency guarantee ---------------------------------

    @Test
    void the_identical_webhook_five_times_confirms_exactly_one_booking() {
        UUID holdGroup = holdService.hold(EVENT_ID, userId, List.of(seatIds.get(3), seatIds.get(4))).holdGroupId();
        BookingResponse booking = checkoutService.checkout(userId, holdGroup, key());

        PaymentWebhookRequest webhook = new PaymentWebhookRequest(
                "evt-" + UUID.randomUUID(), PaymentWebhookRequest.PAYMENT_SUCCEEDED, booking.id());

        List<String> results = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            results.add(paymentWebhookService.handle(webhook));
        }

        assertThat(results.get(0)).isEqualTo("confirmed");
        assertThat(results.subList(1, 5)).containsOnly("duplicate_ignored");

        assertThat(bookingRepository.findById(booking.id()).orElseThrow().getStatus())
                .isEqualTo(BookingStatus.CONFIRMED);
        assertThat(bookingSeatRepository.findSoldSeatIdsIn(booking.seatIds()))
                .as("one booking_seats row per seat, no duplicates")
                .containsExactlyInAnyOrderElementsOf(booking.seatIds());
        assertThat(webhookEventRepository.count()).isEqualTo(1);
    }

    // ---- webhook: the late-payment path ------------------------------------

    @Test
    void a_late_webhook_after_the_hold_expired_cancels_the_booking_and_sells_nothing() {
        UUID holdGroup = holdService.hold(EVENT_ID, userId, List.of(seatIds.get(5))).holdGroupId();
        BookingResponse booking = checkoutService.checkout(userId, holdGroup, key());

        // simulate 8 minutes passing between checkout and the payment confirmation
        seatHoldRepository.findByHoldGroupId(holdGroup).forEach(h -> {
            h.setExpiresAt(Instant.now().minusSeconds(60));
            seatHoldRepository.save(h);
        });

        String result = paymentWebhookService.handle(new PaymentWebhookRequest(
                "evt-" + UUID.randomUUID(), PaymentWebhookRequest.PAYMENT_SUCCEEDED, booking.id()));

        assertThat(result).isEqualTo("hold_expired");
        assertThat(bookingRepository.findById(booking.id()).orElseThrow().getStatus())
                .isEqualTo(BookingStatus.CANCELLED);
        assertThat(bookingSeatRepository.findSoldSeatIdsIn(booking.seatIds())).isEmpty();
    }

    @Test
    void a_payment_failed_webhook_cancels_the_pending_booking() {
        UUID holdGroup = holdService.hold(EVENT_ID, userId, List.of(seatIds.get(6))).holdGroupId();
        BookingResponse booking = checkoutService.checkout(userId, holdGroup, key());

        paymentWebhookService.handle(new PaymentWebhookRequest(
                "evt-" + UUID.randomUUID(), PaymentWebhookRequest.PAYMENT_FAILED, booking.id()));

        assertThat(bookingRepository.findById(booking.id()).orElseThrow().getStatus())
                .isEqualTo(BookingStatus.CANCELLED);
    }

    // ---- HTTP layer -------------------------------------------------------

    @Test
    void checkout_without_the_idempotency_key_header_is_400() throws Exception {
        UUID holdGroup = holdService.hold(EVENT_ID, userId, List.of(seatIds.get(7))).holdGroupId();
        String token = jwtService.issueToken(userRepository.findById(userId).orElseThrow().getEmail(), userId);

        mockMvc.perform(post("/checkout")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"holdGroupId\":\"" + holdGroup + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void the_webhook_endpoint_answers_200_on_every_delivery_including_replays() throws Exception {
        UUID holdGroup = holdService.hold(EVENT_ID, userId, List.of(seatIds.get(8))).holdGroupId();
        BookingResponse booking = checkoutService.checkout(userId, holdGroup, key());
        String body = "{\"providerEventId\":\"evt-http\",\"type\":\"payment_succeeded\",\"bookingId\":" + booking.id() + "}";

        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/webhooks/payment")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk());
        }
        assertThat(bookingRepository.findById(booking.id()).orElseThrow().getStatus())
                .isEqualTo(BookingStatus.CONFIRMED);
    }

    private String key() {
        return "idem-" + UUID.randomUUID();
    }
}
