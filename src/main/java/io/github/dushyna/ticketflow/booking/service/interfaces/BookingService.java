package io.github.dushyna.ticketflow.booking.service.interfaces;

import io.github.dushyna.ticketflow.booking.dto.request.BookingCreateDto;
import io.github.dushyna.ticketflow.booking.dto.response.*;
import io.github.dushyna.ticketflow.user.entity.AppUser;

import java.util.List;
import java.util.UUID;

public interface BookingService {
    PaymentResponseDto createBookings(BookingCreateDto dto, AppUser user);

    List<SeatCoordinateDto> getOccupiedSeats(UUID showtimeId);

    void cancelBookingByStripeSession(String sessionId);

    List<BookingResponseDto> getUserBookings(AppUser user);

    void confirmPayment(String stripeSessionId);

    BookingStatusResponseDto getStatusBySession(String sessionId);

    BoxOfficeOrderResponseDto  sellAtBoxOffice(BookingCreateDto dto, AppUser cashier);

    PaymentResponseDto getPaymentUrlForOrder(UUID orderId);

    TicketVerificationResponseDto verifyTicketEntrance(UUID bookingId);

    List<CashierHistoryResponseDto> getCashierSalesHistory(AppUser cashier);

}
