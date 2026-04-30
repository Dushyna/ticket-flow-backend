package io.github.dushyna.ticketflow.organization.repository;

import io.github.dushyna.ticketflow.organization.entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;
@Repository
public interface OrganizationRepository extends JpaRepository<Organization, UUID> {
    boolean existsBySlug(String slug);

}
