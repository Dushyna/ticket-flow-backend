package io.github.dushyna.ticketflow.user.entity;

import io.github.dushyna.ticketflow.common.BaseEntity;
import io.github.dushyna.ticketflow.organization.entity.Organization;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDate;

/**
 * Application User entity
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "app_user")
public class AppUser extends BaseEntity {

    @Column(name = "password")
    private String password;

    @NotBlank(message = "{user.email.notBlank}")
    @Column(
            name = "email",
            unique = true,
            nullable = false,
            columnDefinition = "VARCHAR(255)"
    )
    private String email;

    @Column(name = "provider")
    private String provider = "LOCAL";

    @Column(name = "provider_id")

    private String providerId;
    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "phone", length = 50)
    private String phone;


    @NotNull(message = "{field.notNull}")
    @Column(name = "confirm_status", nullable = false)
    @ColumnDefault("'UNCONFIRMED'")
    @Enumerated(EnumType.STRING)
    private ConfirmationStatus confirmationStatus = ConfirmationStatus.UNCONFIRMED;

    @NotNull(message = "{field.notNull}")
    @Column(name = "role", nullable = false)
    @Enumerated(EnumType.STRING)
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private Organization organization;

    public AppUser(String password, String email) {
        this.password = password;
        this.email = email;
        this.role = Role.ROLE_USER;
        this.provider = "LOCAL";
    }

    public AppUser(String email, String firstName, String lastName, String provider, String providerId) {
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.provider = provider;
        this.providerId = providerId;
        this.role = Role.ROLE_USER;
        this.confirmationStatus = ConfirmationStatus.CONFIRMED; // OAuth пользователи обычно подтверждены
    }

    @Override
    public String toString() {
        return "AppUser{" +
                "id=" + id +
                ", provider='" + provider + '\'' +
                ", email='" + email + '\'' +
                ", role=" + role +
                '}';
    }

}
