package io.github.dushyna.ticketflow.cinema.repository;

import io.github.dushyna.ticketflow.cinema.entity.MovieHall;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MovieHallRepository extends JpaRepository<MovieHall, UUID> {
    List<MovieHall> findAllByCinemaId(UUID cinemaId);
}
