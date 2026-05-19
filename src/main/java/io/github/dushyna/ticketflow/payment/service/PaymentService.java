package io.github.dushyna.ticketflow.payment.service;

import com.stripe.Stripe;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import io.github.dushyna.ticketflow.booking.entity.Order;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import com.stripe.model.Refund;
import com.stripe.param.RefundCreateParams;

@Service
@Slf4j
public class PaymentService {

    @Value("${stripe.api.key}")
    private String stripeApiKey;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeApiKey;
    }

    public Session  createCheckoutSession(Order order, String movieTitle) throws Exception {
        long amountInCents = order.getTotalPrice()
                .multiply(new java.math.BigDecimal(100))
                .longValue();

        // Sync Stripe expiration with our database 'expiresAt' field.
        // stripeSession.expiresAt must be a Unix timestamp in seconds.
        long expiresAt = order.getExpiresAt().getEpochSecond();

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setExpiresAt(expiresAt)
                .setSuccessUrl("http://localhost:5173/payment/success?session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl("http://localhost:5173/payment/cancel")
                .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
                //.addPaymentMethodType(SessionCreateParams.PaymentMethodType.GIROPAY)
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setQuantity(1L)
                        .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                .setCurrency("eur")
                                .setUnitAmount(amountInCents)
                                .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                        .setName("Tickets for: " + movieTitle)
                                        .setDescription(order.getBookings().size() + " seats booked")
                                        .build())
                                .build())
                        .build())
                .build();

        return Session.create(params);
    }

    public void expireSession(String sessionId) {
        try {
            Session session = Session.retrieve(sessionId);
            session.expire();
            log.info("Stripe session {} has been manually expired", sessionId);
        } catch (Exception e) {
            log.error("Could not expire Stripe session {}: {}", sessionId, e.getMessage());
        }
    }

    public void refundPayment(String sessionId) {
        try {
            // First, retrieve the session to get the PaymentIntent ID
            Session session = Session.retrieve(sessionId);
            String paymentIntentId = session.getPaymentIntent();

            if (paymentIntentId != null) {
                RefundCreateParams params = RefundCreateParams.builder()
                        .setPaymentIntent(paymentIntentId)
                        .build();

                Refund.create(params);
                log.info("Successfully refunded payment for session: {}", sessionId);
            }
        } catch (Exception e) {
            log.error("Refund failed for session {}: {}", sessionId, e.getMessage());
        }
    }
}
