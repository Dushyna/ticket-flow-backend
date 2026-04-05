package io.github.dushyna.ticketflow.cinema.repository;

import io.github.dushyna.ticketflow.cinema.entity.Movie;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MovieRepository extends JpaRepository<Movie, UUID> {}