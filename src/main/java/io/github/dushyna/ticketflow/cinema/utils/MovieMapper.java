package io.github.dushyna.ticketflow.cinema.utils;

import io.github.dushyna.ticketflow.cinema.dto.request.MovieCreateDto;
import io.github.dushyna.ticketflow.cinema.entity.Movie;
import org.mapstruct.*;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface MovieMapper {

    @Mapping(target = "id", ignore = true)
    Movie mapDtoToEntity(MovieCreateDto dto);

    @Mapping(target = "id", ignore = true)
    void updateEntityFromDto(MovieCreateDto dto, @MappingTarget Movie entity);
}
