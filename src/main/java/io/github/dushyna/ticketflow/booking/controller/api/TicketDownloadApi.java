package io.github.dushyna.ticketflow.booking.controller.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.UUID;

@Tag(name = "Ticket Documents", description = "Operations for downloading printable ticket payloads")
@RequestMapping("/api/v1/tickets/download")
public interface TicketDownloadApi {

    @Operation(summary = "Download Printable PDF Order Tickets Book")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping(value = "/order/{orderId}", produces = "application/pdf")
    @ResponseBody
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_TENANT_ADMIN', 'ROLE_CASHIER') or permitAll()")
    byte[] downloadOrderTicketsPdf(@PathVariable UUID orderId);
}
