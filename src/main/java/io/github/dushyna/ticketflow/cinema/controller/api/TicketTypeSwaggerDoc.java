package io.github.dushyna.ticketflow.cinema.controller.api;

import io.github.dushyna.ticketflow.cinema.dto.request.TicketTypeRequestDto;
import io.github.dushyna.ticketflow.cinema.dto.response.TicketTypeResponseDto;
import io.github.dushyna.ticketflow.user.entity.AppUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

@Tag(name = "Ticket Types", description = "Management of ticket categories and pricing multipliers")
public interface TicketTypeSwaggerDoc {

    @Operation(summary = "Create a new ticket type")
    ResponseEntity<TicketTypeResponseDto> create(TicketTypeRequestDto dto, AppUser currentUser);

    @Operation(summary = "Update an existing ticket type")
    ResponseEntity<TicketTypeResponseDto> update(UUID id, TicketTypeRequestDto dto, AppUser currentUser);

    @Operation(summary = "Get current organization's ticket types")
    ResponseEntity<List<TicketTypeResponseDto>> getMyTicketTypes(AppUser currentUser);

    @Operation(summary = "Get ticket types by organization ID (Public)")
    ResponseEntity<List<TicketTypeResponseDto>> getByOrgId(UUID orgId);

    @Operation(summary = "Delete a ticket type")
    ResponseEntity<Void> delete(UUID id, AppUser currentUser);

}





