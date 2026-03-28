package io.github.dushyna.ticketflow.cinema.repository;

import io.github.dushyna.ticketflow.cinema.entity.Cinema;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CinemaRepository extends JpaRepository<Cinema, UUID> {
    List<Cinema> findAllByOrganizationId(UUID organizationId);
}
