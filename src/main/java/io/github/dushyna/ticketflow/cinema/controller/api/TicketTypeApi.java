package io.github.dushyna.ticketflow.cinema.controller.api;

import io.github.dushyna.ticketflow.cinema.dto.request.TicketTypeRequestDto;
import io.github.dushyna.ticketflow.cinema.dto.response.TicketTypeResponseDto;
import io.github.dushyna.ticketflow.exception.handling.response.ErrorResponseDto;
import io.github.dushyna.ticketflow.security.dto.AuthUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Ticket Types", description = "Management of ticket categories (Adult, Child, VIP) and pricing multipliers")
@RequestMapping("/api/v1/ticket-types")
public interface TicketTypeApi {

    @Operation(summary = "Create a new ticket type", description = "Defines a new category like 'Student' or 'Weekend' with a price multiplier.")
    @SecurityRequirement(name = "cookieAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Ticket type created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    TicketTypeResponseDto create(
            @Valid @RequestBody TicketTypeRequestDto dto,
            @Parameter(hidden = true) @AuthenticationPrincipal AuthUserDetails userDetails);

    @Operation(summary = "Update an existing ticket type")
    @SecurityRequirement(name = "cookieAuth")
    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    TicketTypeResponseDto update(
            @Parameter(description = "UUID of the ticket type") @PathVariable UUID id,
            @Valid @RequestBody TicketTypeRequestDto dto,
            @Parameter(hidden = true) @AuthenticationPrincipal AuthUserDetails userDetails);

    @Operation(summary = "Get current organization's ticket types", description = "Returns all categories defined by the owner's organization.")
    @SecurityRequirement(name = "cookieAuth")
    @GetMapping("/my")
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    List<TicketTypeResponseDto> getMyTicketTypes(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthUserDetails userDetails);

    @Operation(summary = "Get ticket types by organization ID", description = "Public endpoint to fetch pricing categories for a specific cinema chain.")
    @GetMapping("/organization/{orgId}")
    @PreAuthorize("permitAll()")
    List<TicketTypeResponseDto> getByOrgId(
            @Parameter(description = "UUID of the organization") @PathVariable UUID orgId);

    @Operation(summary = "Delete a ticket type")
    @SecurityRequirement(name = "cookieAuth")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    void delete(
            @Parameter(description = "UUID of the ticket type") @PathVariable UUID id,
            @Parameter(hidden = true) @AuthenticationPrincipal AuthUserDetails userDetails);
}
