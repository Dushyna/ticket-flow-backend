package io.github.dushyna.ticketflow.cinema.utils;

import io.github.dushyna.ticketflow.cinema.dto.response.CinemaResponseDto;
import io.github.dushyna.ticketflow.cinema.entity.Cinema;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        uses = {MovieHallMapper.class}
)
public interface CinemaMapper {

    @Mapping(target = "organizationId", source = "organization.id")
    @Mapping(target = "halls", source = "halls")
    CinemaResponseDto mapEntityToResponseDto(Cinema entity);
}
