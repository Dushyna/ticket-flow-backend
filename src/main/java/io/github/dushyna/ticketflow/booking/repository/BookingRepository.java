package io.github.dushyna.ticketflow.booking.repository;

import io.github.dushyna.ticketflow.booking.entity.Booking;
import io.github.dushyna.ticketflow.booking.entity.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {

    List<Booking> findAllByShowtimeIdAndStatusIn(UUID showtimeId, List<BookingStatus> statuses);

    boolean existsByShowtimeIdAndRowIndexAndColIndexAndStatusIn(
            UUID showtimeId,
            Integer rowIndex,
            Integer colIndex,
            List<BookingStatus> statuses
    );


    @Query("""
        SELECT count(b) > 0 FROM Booking b
        WHERE b.showtime.id = :showtimeId
        AND b.rowIndex = :row AND b.colIndex = :col
        AND (b.status = 'CONFIRMED'
             OR (b.status = 'PENDING' AND b.createdAt > :threshold))
    """)
    boolean isSeatTaken(@Param("showtimeId") UUID showtimeId,
                        @Param("row") int row,
                        @Param("col") int col,
                        @Param("threshold") Instant threshold);

    @Query("SELECT b FROM Booking b " +
            "JOIN FETCH b.showtime s " +
            "JOIN FETCH s.movie " +
            "JOIN FETCH b.hall " +
            "LEFT JOIN FETCH b.ticketType " +
            "WHERE b.user.id = :userId " +
            "ORDER BY b.createdAt DESC")
    List<Booking> findAllByUserIdWithDetails(@Param("userId") UUID userId);

    @Query("""
    SELECT b FROM Booking b
    WHERE b.showtime.id = :showtimeId
    AND (b.status = 'CONFIRMED'
         OR (b.status = 'PENDING' AND b.expiresAt > CURRENT_TIMESTAMP))
    """)
    List<Booking> findOccupiedOrLocked(@Param("showtimeId") UUID showtimeId);


    @Query("""
    SELECT b FROM Booking b
    JOIN FETCH b.showtime s
    JOIN FETCH s.movie
    JOIN FETCH b.hall
    LEFT JOIN FETCH b.ticketType
    WHERE b.order.id = :orderId
""")
    List<Booking> findAllByOrderIdWithEagerDetails(@Param("orderId") UUID orderId);

}
