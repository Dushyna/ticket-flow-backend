package io.github.dushyna.ticketflow.cinema.controller;

import io.github.dushyna.ticketflow.cinema.controller.api.TicketTypeApi;
import io.github.dushyna.ticketflow.cinema.controller.api.TicketTypeSwaggerDoc;
import io.github.dushyna.ticketflow.cinema.dto.request.TicketTypeRequestDto;
import io.github.dushyna.ticketflow.cinema.dto.response.TicketTypeResponseDto;
import io.github.dushyna.ticketflow.cinema.service.interfaces.TicketTypeService;
import io.github.dushyna.ticketflow.user.entity.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ticket-types")
@RequiredArgsConstructor
public class TicketTypeController implements TicketTypeApi, TicketTypeSwaggerDoc {

    private final TicketTypeService ticketTypeService;

    @Override
    public ResponseEntity<TicketTypeResponseDto> create(TicketTypeRequestDto dto, AppUser currentUser) {
        return new ResponseEntity<>(ticketTypeService.createTicketType(dto, currentUser), HttpStatus.CREATED);
    }

    @Override
    public ResponseEntity<TicketTypeResponseDto> update(UUID id, TicketTypeRequestDto dto, AppUser currentUser) {
        return ResponseEntity.ok(ticketTypeService.updateTicketType(id, dto, currentUser));
    }

    @Override
    public ResponseEntity<List<TicketTypeResponseDto>> getMyTicketTypes(AppUser currentUser) {
        return ResponseEntity.ok(ticketTypeService.getTicketTypesByOrganization(currentUser));
    }

    @Override
    public ResponseEntity<List<TicketTypeResponseDto>> getByOrgId(UUID orgId) {
        return ResponseEntity.ok(ticketTypeService.getTicketTypesByOrganizationId(orgId));
    }

    @Override
    public ResponseEntity<Void> delete(UUID id, AppUser currentUser) {
        ticketTypeService.deleteTicketType(id, currentUser);
        return ResponseEntity.noContent().build();
    }
}
