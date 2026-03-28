package io.github.dushyna.ticketflow.cinema.utils;

import io.github.dushyna.ticketflow.cinema.dto.response.MovieHallResponseDto;
import io.github.dushyna.ticketflow.cinema.entity.MovieHall;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface MovieHallMapper {

    @Mapping(target = "cinemaId", source = "cinema.id")
    MovieHallResponseDto mapEntityToResponseDto(MovieHall entity);
}