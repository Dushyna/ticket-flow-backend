package io.github.dushyna.ticketflow.cinema.utils;

import io.github.dushyna.ticketflow.cinema.dto.request.MovieHallCreateDto;
import io.github.dushyna.ticketflow.cinema.dto.response.MovieHallResponseDto;
import io.github.dushyna.ticketflow.cinema.entity.MovieHall;
import org.mapstruct.*;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface MovieHallMapper {

    @Mapping(target = "cinemaId", source = "cinema.id")
    @Mapping(target = "organizationId", source = "cinema.organization.id")
    MovieHallResponseDto mapEntityToResponseDto(MovieHall entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "cinema", ignore = true)
    @Mapping(target = "rowsCount", source = "rows")
    @Mapping(target = "colsCount", source = "cols")
    MovieHall mapDtoToEntity(MovieHallCreateDto dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "cinema", ignore = true)
    @Mapping(target = "rowsCount", source = "rows")
    @Mapping(target = "colsCount", source = "cols")
    void updateEntityFromDto(MovieHallCreateDto dto, @MappingTarget MovieHall entity);
}
