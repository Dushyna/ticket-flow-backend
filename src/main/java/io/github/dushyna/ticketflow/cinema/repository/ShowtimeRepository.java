package io.github.dushyna.ticketflow.cinema.repository;

import io.github.dushyna.ticketflow.cinema.entity.Showtime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ShowtimeRepository extends JpaRepository<Showtime, UUID> {
    List<Showtime> findAllByHallId(UUID hallId);

    @Query("SELECT COUNT(s) > 0 FROM Showtime s WHERE s.hall.id = :hallId " +
            "AND (:excludeId IS NULL OR s.id <> :excludeId) " +
            "AND s.startTime < :end AND s.endTime > :start")
    boolean existsOverlappingShowtime(
            @Param("hallId") UUID hallId,
            @Param("start") Instant start,
            @Param("end") Instant end,
            @Param("excludeId") UUID excludeId
    );

    List<Showtime> findAllByMovieId(UUID movieId);

    @Query("SELECT s FROM Showtime s " +
            "JOIN FETCH s.movie " +
            "JOIN FETCH s.hall " +
            "WHERE s.hall.cinema.id = :cinemaId " +
            "ORDER BY s.startTime ASC")
    List<Showtime> findAllByCinemaIdWithDetails(@Param("cinemaId") UUID cinemaId);

}