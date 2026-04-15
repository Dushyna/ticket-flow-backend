package io.github.dushyna.ticketflow.cinema.service.interfaces;

import io.github.dushyna.ticketflow.cinema.dto.request.TicketTypeRequestDto;
import io.github.dushyna.ticketflow.cinema.dto.response.TicketTypeResponseDto;
import io.github.dushyna.ticketflow.user.entity.AppUser;

import java.util.List;
import java.util.UUID;

public interface TicketTypeService {

    TicketTypeResponseDto createTicketType(TicketTypeRequestDto dto, AppUser currentUser);

    TicketTypeResponseDto updateTicketType(UUID id, TicketTypeRequestDto dto, AppUser currentUser);

    List<TicketTypeResponseDto> getTicketTypesByOrganization(AppUser currentUser);

    List<TicketTypeResponseDto> getTicketTypesByOrganizationId(UUID organizationId);

    void deleteTicketType(UUID id, AppUser currentUser);
}
