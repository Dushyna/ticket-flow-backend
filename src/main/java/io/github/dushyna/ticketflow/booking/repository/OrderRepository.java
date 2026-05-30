package io.github.dushyna.ticketflow.booking.repository;

import io.github.dushyna.ticketflow.booking.entity.BookingStatus;
import io.github.dushyna.ticketflow.booking.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Pageable;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    Optional<Order> findByStripeSessionId(String stripeSessionId);
    List<Order> findAllByStatusAndExpiresAtBefore(BookingStatus status, Instant time);
    List<Order> findAllByStatusAndCreatedAtBefore(BookingStatus status, Instant time);


    @Query("""
        SELECT DISTINCT o FROM Order o
        JOIN FETCH o.bookings b
        JOIN FETCH b.showtime s
        JOIN FETCH s.movie m
        JOIN FETCH b.hall h
        JOIN h.cinema c
        WHERE o.status = 'CONFIRMED'
        AND c.organization.id = :organizationId
        """)
    List<Order> findTop10ByOrganizationWithBookings(
            @Param("organizationId") UUID organizationId,
            Pageable pageable
    );
}
