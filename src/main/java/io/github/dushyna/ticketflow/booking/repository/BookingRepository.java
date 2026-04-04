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

    @Query("SELECT b FROM Booking b WHERE b.hall.id = :hallId AND b.status IN :statuses")
    List<Booking> findAllActiveBookings(
            @Param("hallId") UUID hallId,
            @Param("statuses") List<BookingStatus> statuses
    );

    boolean existsByHallIdAndRowIndexAndColIndexAndStatusIn(
            UUID hallId,
            Integer rowIndex,
            Integer colIndex,
            List<BookingStatus> statuses
    );
}
