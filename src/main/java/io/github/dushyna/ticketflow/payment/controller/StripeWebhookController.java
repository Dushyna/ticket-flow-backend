package io.github.dushyna.ticketflow.payment.controller;

import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.exception.SignatureVerificationException;
import io.github.dushyna.ticketflow.booking.service.interfaces.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments/webhook")
@RequiredArgsConstructor
@Slf4j
public class StripeWebhookController {

    private final BookingService bookingService;

    @Value("${stripe.webhook.secret}")
    private String endpointSecret;

    @PostMapping
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {

        try {
            Event event = Webhook.constructEvent(payload, sigHeader, endpointSecret);

            if ("checkout.session.completed".equals(event.getType())) {
                log.info("Processing checkout.session.completed event...");

                // 1. Використовуємо прямий метод отримання об'єкта (найбільш надійний)
                Session session = (Session) event.getData().getObject();

                if (session == null) {
                    log.error("Stripe Webhook: Could not extract Session object directly");
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                }

                log.info("Payment confirmed! Stripe Session ID: {}", session.getId());

                // 2. Викликаємо сервіс (тут міняється статус і відправляється лист)
                bookingService.confirmPayment(session.getId());
            }
            else if ("checkout.session.expired".equals(event.getType())) {
                Session session = (Session) event.getData().getObject();
                log.info("Сесія Stripe закінчилася. Звільняємо місця для замовлення: {}", session.getId());
                bookingService.cancelBookingByStripeSession(session.getId());
            }
            return ResponseEntity.ok("OK");
        } catch (SignatureVerificationException e) {
            log.error("Stripe Webhook signature verification failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
        } catch (Exception e) {
            // Тут ми бачили вашу помилку "Empty session object"
            log.error("Error processing Stripe Webhook: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
