package io.github.dushyna.ticketflow.booking.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.dushyna.ticketflow.booking.dto.request.BookingCreateDto;
import io.github.dushyna.ticketflow.booking.dto.request.SeatCoordinateRequestDto;
import io.github.dushyna.ticketflow.booking.dto.response.*;
import io.github.dushyna.ticketflow.booking.entity.Booking;
import io.github.dushyna.ticketflow.booking.entity.BookingStatus;
import io.github.dushyna.ticketflow.booking.repository.BookingRepository;
import io.github.dushyna.ticketflow.booking.repository.OrderRepository;
import io.github.dushyna.ticketflow.booking.service.interfaces.BookingService;
import io.github.dushyna.ticketflow.booking.utils.BookingMapper;
import io.github.dushyna.ticketflow.cinema.entity.MovieHall;
import io.github.dushyna.ticketflow.cinema.entity.Showtime;
import io.github.dushyna.ticketflow.cinema.entity.TicketType;
import io.github.dushyna.ticketflow.cinema.repository.ShowtimeRepository;
import io.github.dushyna.ticketflow.cinema.repository.TicketTypeRepository;
import io.github.dushyna.ticketflow.common.service.TranslationService;
import io.github.dushyna.ticketflow.exception.handling.exceptions.common.RestApiException;
import io.github.dushyna.ticketflow.booking.entity.Order;
import io.github.dushyna.ticketflow.mail.EmailService;
import io.github.dushyna.ticketflow.payment.service.PaymentService;
import io.github.dushyna.ticketflow.user.entity.AppUser;
import io.github.dushyna.ticketflow.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.springframework.http.HttpStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final ShowtimeRepository showtimeRepository;
    private final TicketTypeRepository ticketTypeRepository;
    private final BookingMapper bookingMapper;
    private final ObjectMapper objectMapper;
    private final TranslationService translationService;
    private final OrderRepository orderRepository;
    private final PaymentService paymentService;
    private final EmailService emailService;
    private final UserRepository userRepository;



    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentResponseDto createBookings(BookingCreateDto dto, AppUser currentUser) {
        // 1. Find the showtime
        Showtime showtime = showtimeRepository.findById(dto.showtimeId())
                .orElseThrow(() -> new RestApiException(
                        HttpStatus.NOT_FOUND,
                        translationService.get("showtime.not_found")
                ));

        // 2. Check if the showtime has already started
        Instant now = Instant.now();

        if (showtime.getStartTime().isBefore(now)) {
            throw new RestApiException(
                    HttpStatus.BAD_REQUEST,
                    translationService.get("booking.showtime.already_started") // Добавьте этот ключ в переводчик
            );
        }

        if (showtime.getStartTime().isBefore(now.plus(1, ChronoUnit.HOURS))) {
            throw new RestApiException(
                    HttpStatus.BAD_REQUEST,
                    translationService.get("booking.showtime.closed_pre_sale")
            );
        }

    // --- UPDATED STEP 3: Validate seats using the new expiresAt logic ---
        validateSeatsAvailability(showtime.getId(), dto.seats());


        // --- UPDATED STEP 4: Initialize the Order with expiration time ---
        // System timer is set to 15 minutes.
        // This is the "Hard Lock" that works in the background.
        Instant expirationTime = Instant.now().plus(Duration.ofMinutes(30));

        Order order = new Order();
        order.setUser(currentUser);
        order.setStatus(BookingStatus.PENDING);
        order.setTotalPrice(BigDecimal.ZERO);
        order.setExpiresAt(expirationTime);

        // 5. Prepare hall layout data using the new helper method
        MovieHall hall = showtime.getHall();
        HallLayoutData layoutData = extractHallLayout(hall);

        // 6. SECOND STEP: Create and calculate each booking
        for (var seatDto : dto.seats()) {
            Booking booking = new Booking();
            booking.setUser(currentUser);
            booking.setShowtime(showtime);
            booking.setHall(hall);
            booking.setRowIndex(seatDto.row());
            booking.setColIndex(seatDto.col());
            booking.setStatus(BookingStatus.PENDING);
            booking.setExpiresAt(expirationTime);

            // Call the price calculator using data from our layoutData record
            BigDecimal finalPrice = calculateFinalPrice(seatDto, showtime, layoutData.grid(), layoutData.zoneConfigs());
            booking.setFinalPrice(finalPrice);

            booking.setOrder(order);
            order.getBookings().add(booking);
            order.setTotalPrice(order.getTotalPrice().add(finalPrice));
        }

        // 7. Save the Order (Bookings will be saved automatically due to CascadeType.ALL)
        try {
            orderRepository.save(order);
             orderRepository.flush();
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            log.warn("Concurrent booking attempt detected for seats in showtime: {}", showtime.getId());
            throw new RestApiException(
                    HttpStatus.CONFLICT,
                    translationService.get("booking.seat.taken")
            );
        }
        // 8. THIRD STEP: Initialize Stripe Payment Session
        try {
            // Get the movie title for the Stripe checkout page
            String movieTitle = showtime.getMovie().getTitle();

            // Create session via our PaymentService
            com.stripe.model.checkout.Session session = paymentService.createCheckoutSession(order, movieTitle);

            // Store Stripe Session ID to find this order during Webhook confirmation
            order.setStripeSessionId(session.getId());


            // Return the URL to the frontend for redirection
            return new PaymentResponseDto(session.getUrl());

        } catch (Exception e) {
            // Log and handle Stripe API errors
            log.error("STRIPE ERROR DETAILS: ", e);
            throw new RestApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    translationService.get("payment.init_failed")
            );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponseDto> getUserBookings(AppUser user) {
        return bookingRepository.findAllByUserIdWithDetails(user.getId())
                .stream()
                .map(bookingMapper::toResponseDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SeatCoordinateDto> getOccupiedSeats(UUID showtimeId) {
        // Automatically filters out expired PENDING bookings using the custom query
        return bookingRepository.findOccupiedOrLocked(showtimeId).stream()
                .map(b -> new SeatCoordinateDto(b.getRowIndex(), b.getColIndex()))
                .toList();
    }

    @Override
    @Transactional
    public void cancelBookingByStripeSession(String sessionId) {
        orderRepository.findByStripeSessionId(sessionId).ifPresent(order -> {
            if (order.getStatus() == BookingStatus.PENDING) {
                order.setStatus(BookingStatus.CANCELLED);
                order.getBookings().forEach(b -> b.setStatus(BookingStatus.CANCELLED));
                orderRepository.save(order);
                log.info("Order {} cancelled due to Stripe timeout/failure", order.getId());
            }
        });
    }

    @Override
    @Transactional
    public void confirmPayment(String stripeSessionId) {
        Order order = orderRepository.findByStripeSessionId(stripeSessionId)
                .orElseThrow(() -> new RestApiException(
                        HttpStatus.NOT_FOUND,
                        translationService.get("order.not_found", stripeSessionId)
                ));

        // 1. If the order was already CANCELLED, we check if we can restore it
        if (order.getStatus() == BookingStatus.CANCELLED) {

            // Check if all seats from this order are still free
            boolean allSeatsFree = order.getBookings().stream()
                    .noneMatch(b -> bookingRepository.isSeatTaken(
                            b.getShowtime().getId(),
                            b.getRowIndex(),
                            b.getColIndex(),
                            Instant.now()
                    ));

            if (allSeatsFree) {
                order.setStatus(BookingStatus.PENDING);
            } else {
                // ACTION 1: Initiate actual refund
                paymentService.refundPayment(stripeSessionId);

                // ACTION 2: Update status to a specific one if you have it,
                // e.g., REFUNDED, or keep CANCELLED but log it.
                order.setStatus(BookingStatus.CANCELLED);
                orderRepository.save(order);

                // ACTION 3: Send "Sorry" email
                emailService.sendRefundNotificationEmail(order);
                return;
            }
        }

        // 2. Final Confirmation (Works for both PENDING and Restored orders)
        order.setStatus(BookingStatus.CONFIRMED);

        order.getBookings().forEach(b -> {
            b.setStatus(BookingStatus.CONFIRMED);

            // Preload data for the email service
            Hibernate.initialize(b.getShowtime());
            if (b.getShowtime() != null) {
                Hibernate.initialize(b.getShowtime().getMovie());
            }
            Hibernate.initialize(b.getHall());
        });

        // 3. Save everything to the Database
        orderRepository.save(order);

        // 4. Send the tickets to the user
        try {
            emailService.sendBookingConfirmationEmail(order.getId());
        } catch (Exception e) {
            log.error("Failed to send email for order {}", order.getId(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public BookingStatusResponseDto getStatusBySession(String sessionId) {
        Order order = orderRepository.findByStripeSessionId(sessionId)
                .orElseThrow(() -> new RestApiException(HttpStatus.NOT_FOUND, "Order not found"));
        return new BookingStatusResponseDto(order.getStatus().name());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BoxOfficeOrderResponseDto  sellAtBoxOffice(BookingCreateDto dto, AppUser cashier) {
        if (cashier == null) {
            org.springframework.security.core.Authentication auth =
                    org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();

            if (auth != null && auth.getName() != null) {
                cashier = userRepository.findByEmailIgnoreCase(auth.getName())
                        .orElseThrow(() -> new RestApiException(
                                HttpStatus.NOT_FOUND,
                                "Authenticated cashier account user profile not found in database"
                        ));
            }
        }

        if (cashier == null) {
            throw new RestApiException(
                    HttpStatus.UNAUTHORIZED,
                    "Access Denied: Current session context lacks a valid authenticated cashier profile identity."
            );
        }
        // 1. Find the showtime
        Showtime showtime = showtimeRepository.findById(dto.showtimeId())
                .orElseThrow(() -> new RestApiException(
                        HttpStatus.NOT_FOUND,
                        translationService.get("showtime.not_found")
                ));

        // 2. Validate seats using the expiresAt logic
        validateSeatsAvailability(showtime.getId(), dto.seats());

        // 3. Initialize the Order immediately as CONFIRMED (No Stripe session needed)
        Order order = new Order();
        order.setUser(cashier);
        order.setStatus(BookingStatus.CONFIRMED);
        order.setTotalPrice(BigDecimal.ZERO);
        order.setExpiresAt(null);

        // 4. Prepare hall layout data for price calculations
        MovieHall hall = showtime.getHall();
        HallLayoutData layoutData = extractHallLayout(hall);

        // 5. Create and calculate each booking directly as CONFIRMED
        for (var seatDto : dto.seats()) {
            Booking booking = new Booking();
            booking.setUser(cashier);
            booking.setShowtime(showtime);
            booking.setHall(hall);
            booking.setRowIndex(seatDto.row());
            booking.setColIndex(seatDto.col());
            booking.setStatus(BookingStatus.CONFIRMED);
            booking.setExpiresAt(null);

            // Call the price calculator using data from our layoutData record
            BigDecimal finalPrice = calculateFinalPrice(seatDto, showtime, layoutData.grid(), layoutData.zoneConfigs());
            booking.setFinalPrice(finalPrice);

            booking.setOrder(order);
            order.getBookings().add(booking);
            order.setTotalPrice(order.getTotalPrice().add(finalPrice));
        }

        // 6. Save the finalized Order and Bookings to the database
        try {
            orderRepository.save(order);
            orderRepository.flush();
            log.info("BOX OFFICE: Order {} successfully completed by cashier {}", order.getId(), cashier.getEmail());
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            log.warn("Box office conflict: Seat already taken for showtime: {}", showtime.getId());
            throw new RestApiException(
                    HttpStatus.CONFLICT,
                    translationService.get("booking.seat.taken") // Повертаємо красиву помилку
            );
        }

        List<UUID> generatedBookingIds = order.getBookings().stream()
                .map(io.github.dushyna.ticketflow.common.BaseEntity::getId) // Assumes BaseEntity provides getId()
                .toList();

        return new BoxOfficeOrderResponseDto(order.getId(), generatedBookingIds);
    }


    @Override
    @Transactional
    public PaymentResponseDto getPaymentUrlForOrder(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RestApiException(HttpStatus.NOT_FOUND,
                        translationService.get("order.not_found", orderId.toString())));

        // 1. If the order is already CANCELLED or CONFIRMED, no payment allowed
        if (order.getStatus() != BookingStatus.PENDING) {
            throw new RestApiException(HttpStatus.BAD_REQUEST, "Payment is no longer available for this order.");
        }

        // 2. Check if the HARD LOCK (30 min) has already passed
        if (order.getExpiresAt().isBefore(Instant.now())) {
            // If time is up, we mark it as CANCELLED immediately
            order.setStatus(BookingStatus.CANCELLED);
            order.getBookings().forEach(b -> b.setStatus(BookingStatus.CANCELLED));
            orderRepository.save(order);

            throw new RestApiException(HttpStatus.GONE, "Your reservation has expired. Please book again.");
        }

        try {
            // 3. Retrieve the existing session from Stripe
            com.stripe.model.checkout.Session session =
                    com.stripe.model.checkout.Session.retrieve(order.getStripeSessionId());

            // 4. If Stripe says it's still 'open', we return the URL
            if ("open".equals(session.getStatus())) {
                return new PaymentResponseDto(session.getUrl());
            } else {
                // If Stripe session is closed for any reason, but our DB hasn't canceled it yet
                throw new RestApiException(HttpStatus.GONE, "Payment session is no longer active.");
            }

        } catch (Exception e) {
            log.error("STRIPE ERROR: Failed to retrieve session for order {}", orderId, e);
            throw new RestApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not load payment page.");
        }
    }


    @Override
    @Transactional
    public TicketVerificationResponseDto verifyTicketEntrance(UUID bookingId) {
        // 1. Check if the ticket exists at all
        Booking booking = bookingRepository.findById(bookingId).orElse(null);
        if (booking == null) {
            return new TicketVerificationResponseDto(false, "TICKET_NOT_FOUND", null, null, null);
        }

        String movieTitle = booking.getShowtime().getMovie().getTitle();
        String seatInfo = String.format("Row: %d, Seat: %d", booking.getRowIndex() + 1, booking.getColIndex() + 1);
        Instant startTime = booking.getShowtime().getStartTime();

        // 2. Check if the ticket is paid (CONFIRMED)
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            return new TicketVerificationResponseDto(false, "TICKET_NOT_PAID", movieTitle, seatInfo, startTime);
        }

        // 3. THE ANTI-FRAUD SHIELD: Prevent entering twice with the same QR code copy
        if (booking.isCheckedIn()) {
            return new TicketVerificationResponseDto(false, "ALREADY_SCANNED", movieTitle, seatInfo, startTime);
        }

        // 4. Strict time check: Prevent entering a movie that took place in the past
        // Allow entry up to 30 minutes before the movie starts and until it ends (e.g., 3 hours buffer)
        Instant now = Instant.now();
        Instant allowedEarliest = startTime.minus(java.time.Duration.ofMinutes(30));
        Instant allowedLatest = startTime.plus(java.time.Duration.ofHours(3));

        if (now.isBefore(allowedEarliest)) {
            return new TicketVerificationResponseDto(false, "TOO_EARLY_FOR_SHOWTIME", movieTitle, seatInfo, startTime);
        }
        if (now.isAfter(allowedLatest)) {
            return new TicketVerificationResponseDto(false, "SHOWTIME_ALREADY_PASSED", movieTitle, seatInfo, startTime);
        }

        // 5. SUCCESS FLOW: Mark the ticket as used and lock it
        booking.setCheckedIn(true);
        booking.setCheckedInAt(now);
        bookingRepository.save(booking);

        log.info("GATE CONTROL: Ticket {} successfully scanned. Access Granted for seat {}", bookingId, seatInfo);
        return new TicketVerificationResponseDto(true, "ACCESS_GRANTED", movieTitle, seatInfo, startTime);
    }

    private BigDecimal calculateFinalPrice(
            SeatCoordinateRequestDto seatDto,
            Showtime showtime,
            List<List<String>> grid,
            List<Map<String, Object>> zoneConfigs
    ) {
        // 1. Calculate zone price multiplier based on the hall layout grid
        String seatZoneId = grid.get(seatDto.row()).get(seatDto.col());
        BigDecimal zoneMultiplier = zoneConfigs.stream()
                .filter(config -> config.get("id").equals(seatZoneId))
                .map(config -> new BigDecimal(config.getOrDefault("multiplier", "1").toString()))
                .findFirst()
                .orElse(BigDecimal.ONE);

        // 2. Calculate ticket type discount/multiplier if applicable
        BigDecimal ticketMultiplier = BigDecimal.ONE;
        if (seatDto.ticketTypeId() != null) { // This method resolves perfectly now
            TicketType tt = ticketTypeRepository.findById(seatDto.ticketTypeId())
                    .orElseThrow(() -> new RestApiException(
                            HttpStatus.NOT_FOUND,
                            translationService.get("ticket_type.not_found")
                    ));
            ticketMultiplier = tt.getDiscount();
        }

        // 3. Return the cumulative final price for this specific seat
        return showtime.getBasePrice()
                .multiply(zoneMultiplier)
                .multiply(ticketMultiplier);
    }

    private void validateSeatsAvailability(UUID showtimeId, List<SeatCoordinateRequestDto> seats) {
        // Check if any of the requested seats are already taken or actively held
        for (var seatDto : seats) {
            if (bookingRepository.isSeatTaken(showtimeId, seatDto.row(), seatDto.col(), Instant.now())) {
                String errorMessage = translationService.get(
                        "booking.seat.taken",
                        seatDto.row() + 1,
                        seatDto.col() + 1
                );
                throw new RestApiException(HttpStatus.CONFLICT, errorMessage);
            }
        }
    }

    private HallLayoutData extractHallLayout(MovieHall hall) {
        Map<String, Object> layout = hall.getLayoutConfig();
        List<List<String>> grid = objectMapper.convertValue(layout.get("grid"), new TypeReference<>() {});
        List<Map<String, Object>> zoneConfigs = objectMapper.convertValue(layout.get("zoneConfigs"), new TypeReference<>() {});
        return new HallLayoutData(grid, zoneConfigs);
    }


    @Override
    @Transactional(readOnly = true)
    public List<CashierHistoryResponseDto> getCashierSalesHistory(AppUser cashier) {
        if (cashier == null) {
            org.springframework.security.core.Authentication auth =
                    org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();

            if (auth != null && auth.getName() != null) {
                // Re-load your authenticated cashier directly from the DB by email string safely
                cashier = userRepository.findByEmailIgnoreCase(auth.getName()).orElse(null);
            }
        }

        // 1. Strict verification anchor
        if (cashier == null || cashier.getOrganization() == null) {
            throw new RestApiException(HttpStatus.UNAUTHORIZED, "Unauthorized or missing organization context");
        }

        // 2. Fetch data from DB using the updated path layout parameters
        List<Order> cashierOrders = orderRepository.findTop10ByOrganizationWithBookings(
                cashier.getOrganization().getId(),
                org.springframework.data.domain.PageRequest.of(0, 10, org.springframework.data.domain.Sort.by("createdAt").descending())
        );

        // 3. Map safely into report dto nodes
        return cashierOrders.stream()
                .map(order -> {
                    var bookings = order.getBookings();
                    var firstBooking = bookings.isEmpty() ? null : bookings.getFirst();

                    String movieTitle = firstBooking != null ? firstBooking.getShowtime().getMovie().getTitle() : "N/A";
                    String hallName = firstBooking != null ? firstBooking.getHall().getName() : "N/A";
                    int ticketsCount = bookings.size();

                    return new CashierHistoryResponseDto(
                            order.getId(),
                            movieTitle,
                            hallName,
                            ticketsCount,
                            order.getTotalPrice(),
                            order.getCreatedAt()
                    );
                })
                .toList();
    }

}
