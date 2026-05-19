package io.github.dushyna.ticketflow.booking.service.impl;

import com.lowagie.text.*;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import io.github.dushyna.ticketflow.booking.entity.Booking;
import io.github.dushyna.ticketflow.booking.repository.BookingRepository;
import io.github.dushyna.ticketflow.booking.service.interfaces.TicketDocumentService; // Added interface import
import io.github.dushyna.ticketflow.exception.handling.exceptions.common.RestApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class TicketDocumentServiceImpl implements TicketDocumentService { // Implements contract interface

    private final BookingRepository bookingRepository;

    @Override
    public byte[] generateQrCodeImage(String ticketId) throws Exception {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(ticketId, BarcodeFormat.QR_CODE, 200, 200);

        ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
        return pngOutputStream.toByteArray();
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generateOrderTicketsPdf(UUID orderId) {
        // 1. Service queries the database using eager details fetch execution logic
        List<Booking> bookings = bookingRepository.findAllByOrderIdWithEagerDetails(orderId);

        // 2. Business validation barrier logic check execution
        if (bookings.isEmpty()) {
            throw new RestApiException(HttpStatus.NOT_FOUND, "No tickets found for this order allocation.");
        }

        // 3. Document PDF construction loop starts here
        java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream();
        com.lowagie.text.Document document = new com.lowagie.text.Document(com.lowagie.text.PageSize.A6);

        try {
            com.lowagie.text.pdf.PdfWriter.getInstance(document, outputStream);
            document.open();

            for (int i = 0; i < bookings.size(); i++) {
                Booking booking = bookings.get(i);

                com.lowagie.text.Font titleFont = com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA_BOLD, 16);
                com.lowagie.text.Paragraph header = new com.lowagie.text.Paragraph("TICKETFLOW", titleFont);
                header.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
                document.add(header);
                document.add(new com.lowagie.text.Paragraph("--------------------------------------------------"));

                com.lowagie.text.Font infoFont = com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA, 10);
                com.lowagie.text.Font boldFont = com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA_BOLD, 11);

                document.add(new com.lowagie.text.Paragraph("Movie: " + booking.getShowtime().getMovie().getTitle(), boldFont));
                document.add(new com.lowagie.text.Paragraph("Hall: " + booking.getHall().getName(), infoFont));

                java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(java.time.ZoneId.systemDefault());
                document.add(new com.lowagie.text.Paragraph("Time: " + formatter.format(booking.getShowtime().getStartTime()), infoFont));
                document.add(new com.lowagie.text.Paragraph("Row: " + (booking.getRowIndex() + 1) + "  Seat: " + (booking.getColIndex() + 1), boldFont));
                document.add(new com.lowagie.text.Paragraph("Price: " + booking.getFinalPrice() + " EUR", infoFont));
                document.add(new com.lowagie.text.Paragraph("Type: " + (booking.getTicketType() != null ? booking.getTicketType().getLabel() : "Standard"), infoFont));

                document.add(new com.lowagie.text.Paragraph(" "));

                byte[] qrBytes = generateQrCodeImage(booking.getId().toString());
                com.lowagie.text.Image qrImage = com.lowagie.text.Image.getInstance(qrBytes);
                qrImage.setAlignment(com.lowagie.text.Image.ALIGN_CENTER);
                document.add(qrImage);

                com.lowagie.text.Font footerFont = com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA, 8);
                com.lowagie.text.Paragraph footer = new com.lowagie.text.Paragraph("Ticket " + (i + 1) + " of " + bookings.size() + " | ID: " + booking.getId(), footerFont);
                footer.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
                document.add(footer);

                if (i < bookings.size() - 1) {
                    document.newPage();
                }
            }

            document.close();
            log.info("PDF Multi-page Order Ticket document successfully generated.");
        } catch (Exception e) {
            log.error("FAILED to compile composite order tickets document asset payload: ", e);
        }

        return outputStream.toByteArray();
    }}
