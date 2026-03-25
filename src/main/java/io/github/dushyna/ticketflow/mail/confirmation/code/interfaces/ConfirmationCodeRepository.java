package io.github.dushyna.ticketflow.mail.confirmation.code.interfaces;

import io.github.dushyna.ticketflow.mail.confirmation.code.ConfirmationCode;
import io.github.dushyna.ticketflow.user.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConfirmationCodeRepository extends JpaRepository<ConfirmationCode, UUID> {

    Optional<ConfirmationCode> findByUser(AppUser user);

    void deleteByUser(AppUser user);
}
