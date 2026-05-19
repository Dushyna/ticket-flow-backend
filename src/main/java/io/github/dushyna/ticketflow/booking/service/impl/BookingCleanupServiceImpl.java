package io.github.dushyna.ticketflow.booking.service.impl;

import com.stripe.model.checkout.Session;
import io.github.dushyna.ticketflow.booking.entity.BookingStatus;
import io.github.dushyna.ticketflow.booking.entity.Order;
import io.github.dushyna.ticketflow.booking.repository.OrderRepository;
import io.github.dushyna.ticketflow.booking.service.interfaces.BookingCleanupService;
import io.github.dushyna.ticketflow.booking.service.interfaces.BookingService;
import io.github.dushyna.ticketflow.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingCleanupServiceImpl implements BookingCleanupService {

    private final OrderRepository orderRepository;
    private final BookingService bookingService;
    private final PaymentService paymentService;

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void cleanupExpiredOrders() {
        // Now we use the actual expiresAt field from your DB
        Instant now = Instant.now();

        log.info("Starting cleanup check at {}", now);

        // Find orders that are PENDING and already passed their expiration time
        List<Order> expiredOrders = orderRepository
                .findAllByStatusAndExpiresAtBefore(BookingStatus.PENDING, now);

        for (Order order : expiredOrders) {
            try {
                Session session = Session.retrieve(order.getStripeSessionId());
                String stripeStatus = session.getStatus();

                if ("open".equals(stripeStatus)) {
                    // ACTION: The user hasn't paid yet, and time is up.
                    // 1. Force Stripe to close the payment page
                    paymentService.expireSession(order.getStripeSessionId());

                    // 2. Mark as CANCELLED in our DB
                    order.setStatus(BookingStatus.CANCELLED);
                    order.getBookings().forEach(b -> b.setStatus(BookingStatus.CANCELLED));
                    orderRepository.save(order);
                    log.info("Order {} hard-expired and released", order.getId());

                } else if ("complete".equals(stripeStatus)) {
                    // Payment was successful but webhook is late
                    bookingService.confirmPayment(order.getStripeSessionId());

                } else if ("expired".equals(stripeStatus)) {
                    // Already expired by Stripe's own timer
                    order.setStatus(BookingStatus.CANCELLED);
                    order.getBookings().forEach(b -> b.setStatus(BookingStatus.CANCELLED));
                    orderRepository.save(order);
                }

            } catch (Exception e) {
                log.error("Cleanup error for Order {}: {}", order.getId(), e.getMessage());
            }
        }
    }
}
