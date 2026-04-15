package io.github.dushyna.ticketflow.user.repository;

import io.github.dushyna.ticketflow.user.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<AppUser, UUID> {

    @Query("select a from AppUser a where upper(a.email) = upper(?1)")
    Optional<AppUser> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);

    @Query("SELECT u FROM AppUser u LEFT JOIN FETCH u.organization WHERE UPPER(u.email) = UPPER(?1)")
    Optional<AppUser> findByEmailWithOrganization(String email);
}
