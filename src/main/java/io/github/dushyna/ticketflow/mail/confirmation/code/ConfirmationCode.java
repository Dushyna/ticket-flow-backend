package io.github.dushyna.ticketflow.mail.confirmation.code;

import io.github.dushyna.ticketflow.user.entity.AppUser;
import io.github.dushyna.ticketflow.common.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

import static io.github.dushyna.ticketflow.common.EntityUtil.getIdForToString;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "confirm_code")
public class ConfirmationCode extends BaseEntity {

    @NotNull
    @Column(name = "expired", nullable = false)
    private Instant expired;

    @NotNull
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, updatable = false, unique = true)
    private AppUser user;

    public ConfirmationCode(Instant expired, AppUser user) {
        this.expired = expired;
        this.user = user;
    }

    @Override
    public String toString() {
        return "ConfirmationCode{" +
                "code=" + id +
                ", expired=" + expired +
                ", appUserId=" + getIdForToString(user) +
                '}';
    }
}
