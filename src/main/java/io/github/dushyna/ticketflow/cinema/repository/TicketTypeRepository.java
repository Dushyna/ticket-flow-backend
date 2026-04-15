package io.github.dushyna.ticketflow.cinema.repository;

import io.github.dushyna.ticketflow.cinema.entity.TicketType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TicketTypeRepository extends JpaRepository<TicketType, UUID> {
    List<TicketType> findAllByOrganizationId(UUID organizationId);
}
