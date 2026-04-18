package io.github.dushyna.ticketflow.cinema.entity;

import io.github.dushyna.ticketflow.common.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.util.Map;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "movie_halls")
public class MovieHall extends BaseEntity {

    @NotBlank(message = "{hall.name.notBlank}")
    @Column(name = "name", nullable = false)
    private String name;

    @NotNull(message = "{field.notNull}")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cinema_id", nullable = false)
    private Cinema cinema;

    @Min(value = 1, message = "{hall.rows.min}")
    @Column(name = "rows_count", nullable = false)
    private Integer rowsCount;

    @Min(value = 1, message = "{hall.cols.min}")
    @Column(name = "cols_count", nullable = false)
    private Integer colsCount;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "layout_config", nullable = false)
    private Map<String, Object> layoutConfig;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private java.time.Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private java.time.Instant updatedAt;


    public MovieHall(String name, Cinema cinema, Integer rowsCount, Integer colsCount, Map<String, Object> layoutConfig) {
        this.name = name;
        this.cinema = cinema;
        this.rowsCount = rowsCount;
        this.colsCount = colsCount;
        this.layoutConfig = layoutConfig;
    }
    @Override
    public String toString() {
        return "MovieHall{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", rowsCount=" + rowsCount +
                ", colsCount=" + colsCount +
                ", cinemaId=" + (cinema != null ? cinema.getId() : "null") +
                '}';
    }


}
