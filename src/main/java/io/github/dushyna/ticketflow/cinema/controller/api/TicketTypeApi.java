package io.github.dushyna.ticketflow.cinema.controller.api;

import io.github.dushyna.ticketflow.cinema.dto.request.TicketTypeRequestDto;
import io.github.dushyna.ticketflow.cinema.dto.response.TicketTypeResponseDto;
import io.github.dushyna.ticketflow.user.entity.AppUser;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;

public interface TicketTypeApi {

    @PostMapping
    ResponseEntity<TicketTypeResponseDto> create(
            @Valid @RequestBody TicketTypeRequestDto dto,
            @AuthenticationPrincipal AppUser currentUser);

    @PatchMapping("/{id}")
    ResponseEntity<TicketTypeResponseDto> update(
            @PathVariable UUID id,
            @Valid @RequestBody TicketTypeRequestDto dto,
            @AuthenticationPrincipal AppUser currentUser);

    @GetMapping("/my")
    ResponseEntity<List<TicketTypeResponseDto>> getMyTicketTypes(
            @AuthenticationPrincipal AppUser currentUser);

    @GetMapping("/organization/{orgId}")
    ResponseEntity<List<TicketTypeResponseDto>> getByOrgId(@PathVariable UUID orgId);

    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal AppUser currentUser);
}
