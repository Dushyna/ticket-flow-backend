package io.github.dushyna.ticketflow.booking.repository;

import io.github.dushyna.ticketflow.booking.entity.Booking;
import io.github.dushyna.ticketflow.booking.entity.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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

    @Query("SELECT b FROM Booking b " +
            "JOIN FETCH b.showtime s " +
            "JOIN FETCH s.movie " +
            "JOIN FETCH b.hall " +
            "LEFT JOIN FETCH b.ticketType " +
            "WHERE b.user.id = :userId " +
            "ORDER BY b.createdAt DESC")
    List<Booking> findAllByUserIdWithDetails(@Param("userId") UUID userId);
}
