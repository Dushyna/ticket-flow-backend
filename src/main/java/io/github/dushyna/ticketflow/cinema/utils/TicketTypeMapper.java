package io.github.dushyna.ticketflow.cinema.utils;

import io.github.dushyna.ticketflow.cinema.dto.request.TicketTypeRequestDto;
import io.github.dushyna.ticketflow.cinema.dto.response.TicketTypeResponseDto;
import io.github.dushyna.ticketflow.cinema.entity.TicketType;
import org.mapstruct.*;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TicketTypeMapper {

    TicketTypeResponseDto mapEntityToResponseDto(TicketType entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "organization", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    TicketType mapRequestDtoToEntity(TicketTypeRequestDto dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "organization", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromDto(TicketTypeRequestDto dto, @MappingTarget TicketType entity);
}
