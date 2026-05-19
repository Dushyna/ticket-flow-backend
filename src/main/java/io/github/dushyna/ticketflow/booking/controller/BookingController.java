package io.github.dushyna.ticketflow.booking.controller;

import io.github.dushyna.ticketflow.booking.controller.api.BookingApi;
import io.github.dushyna.ticketflow.booking.dto.request.BookingCreateDto;
import io.github.dushyna.ticketflow.booking.dto.response.*;
import io.github.dushyna.ticketflow.booking.service.interfaces.BookingService;
import io.github.dushyna.ticketflow.security.dto.AuthUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class BookingController implements BookingApi {

    private final BookingService bookingService;

    @Override
    public PaymentResponseDto create(BookingCreateDto dto,
                                     @AuthenticationPrincipal AuthUserDetails userDetails) {
        return bookingService.createBookings(dto, userDetails.user());
    }

    @Override
    public List<SeatCoordinateDto> getOccupied(UUID showtimeId) {
        return bookingService.getOccupiedSeats(showtimeId);
    }

    @Override
    public List<BookingResponseDto> getMyBookings(@AuthenticationPrincipal AuthUserDetails userDetails) {
        return bookingService.getUserBookings(userDetails.user());
    }

    @Override
    public BookingStatusResponseDto getStatusBySession(String sessionId) {
        return bookingService.getStatusBySession(sessionId);
    }

    @Override
    public BoxOfficeOrderResponseDto  sellAtBoxOffice(BookingCreateDto dto, AuthUserDetails userDetails) {
       return bookingService.sellAtBoxOffice(dto, userDetails.user());
    }

    @Override
    public PaymentResponseDto getPaymentUrl(UUID orderId) {
        return bookingService.getPaymentUrlForOrder(orderId);
    }

    @Override
    public TicketVerificationResponseDto verifyEntrance(UUID bookingId) {
        return bookingService.verifyTicketEntrance(bookingId);
    }

    // Inside BookingController.java

    @Override
    public List<CashierHistoryResponseDto> getCashierSalesHistory(AuthUserDetails userDetails) {
        return bookingService.getCashierSalesHistory(userDetails.user());
    }

}
