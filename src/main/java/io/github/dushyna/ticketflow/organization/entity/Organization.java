package io.github.dushyna.ticketflow.organization.entity;

import io.github.dushyna.ticketflow.common.BaseEntity;
import io.github.dushyna.ticketflow.user.entity.AppUser;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "organizations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Organization extends BaseEntity {
    @NotBlank
    @Column(nullable = false, unique = true)
    private String name;

    @NotBlank
    @Pattern(regexp = "^[a-z0-9-]+$", message = "Slug must contain only lowercase letters, numbers, and hyphens")
    @Column(nullable = false, unique = true)
    private String slug;

    private String contactEmail;

    @OneToMany(mappedBy = "organization")
    @Builder.Default
    private List<AppUser> employees = new java.util.ArrayList<>();


    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;


    @Override
    public String toString() {
        return "Organization{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", slug='" + slug + '\'' +
                ", contactEmail='" + contactEmail + '\'' +
                ", createdAt=" + String.valueOf(createdAt) +
                ", updatedAt=" + String.valueOf(updatedAt) +
                "}";
    }
}
