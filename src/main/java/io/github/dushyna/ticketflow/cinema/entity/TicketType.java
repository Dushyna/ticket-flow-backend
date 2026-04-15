package io.github.dushyna.ticketflow.cinema.entity;

import io.github.dushyna.ticketflow.common.BaseEntity;
import io.github.dushyna.ticketflow.organization.entity.Organization;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

@Entity
@Table(name = "ticket_types")
@Getter
@Setter
@NoArgsConstructor
@ToString
public class TicketType extends BaseEntity {

    @Column(nullable = false)
    private String label; // "Student", "Child", "Adult"

    @Column(nullable = false, precision = 3, scale = 2)
    private BigDecimal discount; // 1.0, 0.8, 0.5

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(name = "is_default")
    private boolean isDefault = false;

    @org.hibernate.annotations.CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private java.time.Instant createdAt;

    @org.hibernate.annotations.UpdateTimestamp
    @Column(name = "updated_at")
    private java.time.Instant updatedAt;

}

