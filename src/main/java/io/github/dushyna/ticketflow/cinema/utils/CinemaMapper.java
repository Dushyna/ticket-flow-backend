package io.github.dushyna.ticketflow.cinema.utils;

import io.github.dushyna.ticketflow.cinema.dto.request.CinemaCreateDto;
import io.github.dushyna.ticketflow.cinema.dto.response.CinemaResponseDto;
import io.github.dushyna.ticketflow.cinema.entity.Cinema;
import org.mapstruct.*;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        uses = {MovieHallMapper.class},
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface CinemaMapper {

    @Mapping(target = "organizationId", source = "organization.id")
    @Mapping(target = "halls", source = "halls")
    CinemaResponseDto mapEntityToResponseDto(Cinema entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "halls", ignore = true)
    @Mapping(target = "organization", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Cinema mapDtoToEntity(CinemaCreateDto dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "halls", ignore = true)
    @Mapping(target = "organization", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromDto(CinemaCreateDto dto, @MappingTarget Cinema entity);
}
