package io.github.dushyna.ticketflow.booking.service.interfaces;

import java.util.UUID;

public interface TicketDocumentService {

    byte[] generateQrCodeImage(String ticketId) throws Exception;

    byte[] generateOrderTicketsPdf(UUID orderId);}
