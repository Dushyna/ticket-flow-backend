package io.github.dushyna.ticketflow.cinema.controller;

import io.github.dushyna.ticketflow.cinema.controller.api.TicketTypeApi;
import io.github.dushyna.ticketflow.cinema.dto.request.TicketTypeRequestDto;
import io.github.dushyna.ticketflow.cinema.dto.response.TicketTypeResponseDto;
import io.github.dushyna.ticketflow.cinema.service.interfaces.TicketTypeService;
import io.github.dushyna.ticketflow.security.dto.AuthUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class TicketTypeController implements TicketTypeApi {

    private final TicketTypeService ticketTypeService;

    @Override
    public TicketTypeResponseDto create(TicketTypeRequestDto dto,
                                        @AuthenticationPrincipal AuthUserDetails userDetails) {
        return ticketTypeService.createTicketType(dto, userDetails.user());
    }

    @Override
    public TicketTypeResponseDto update(UUID id, TicketTypeRequestDto dto,
                                        @AuthenticationPrincipal AuthUserDetails userDetails) {
        return ticketTypeService.updateTicketType(id, dto, userDetails.user());
    }

    @Override
    public List<TicketTypeResponseDto> getMyTicketTypes(@AuthenticationPrincipal AuthUserDetails userDetails) {
        return ticketTypeService.getTicketTypesByOrganization(userDetails.user());
    }

    @Override
    public List<TicketTypeResponseDto> getByOrgId(UUID orgId) {
        return ticketTypeService.getTicketTypesByOrganizationId(orgId);
    }

    @Override
    public void delete(UUID id, @AuthenticationPrincipal AuthUserDetails userDetails) {
        ticketTypeService.deleteTicketType(id, userDetails.user());
    }
}
