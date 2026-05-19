package io.github.dushyna.ticketflow.booking.controller;

import io.github.dushyna.ticketflow.booking.controller.api.TicketDownloadApi;
import io.github.dushyna.ticketflow.booking.service.interfaces.TicketDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class TicketDownloadController implements TicketDownloadApi {

    private final TicketDocumentService ticketDocumentService;

    @Override
    public byte[] downloadOrderTicketsPdf(UUID orderId) {
        return ticketDocumentService.generateOrderTicketsPdf(orderId);
    }
}
