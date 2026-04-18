package io.github.dushyna.ticketflow.cinema.repository;

import io.github.dushyna.ticketflow.cinema.entity.TicketType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public interface TicketTypeRepository extends JpaRepository<TicketType, UUID> {
    List<TicketType> findAllByOrganizationId(UUID organizationId);

    @Modifying
    @Transactional
    @Query("UPDATE TicketType t SET t.isDefault = false WHERE t.organization.id = :orgId")
    void resetDefaultsByOrganizationId(@Param("orgId") UUID orgId);

}
