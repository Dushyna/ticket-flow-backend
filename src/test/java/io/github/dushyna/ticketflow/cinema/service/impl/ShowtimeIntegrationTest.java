package io.github.dushyna.ticketflow.cinema.service.impl;

import io.github.dushyna.ticketflow.cinema.dto.request.ShowtimeCreateDto;
import io.github.dushyna.ticketflow.cinema.dto.response.ShowtimeResponseDto;
import io.github.dushyna.ticketflow.cinema.entity.Cinema;
import io.github.dushyna.ticketflow.cinema.entity.Movie;
import io.github.dushyna.ticketflow.cinema.entity.MovieHall;
import io.github.dushyna.ticketflow.cinema.entity.Showtime;
import io.github.dushyna.ticketflow.cinema.repository.CinemaRepository;
import io.github.dushyna.ticketflow.cinema.repository.MovieHallRepository;
import io.github.dushyna.ticketflow.cinema.repository.MovieRepository;
import io.github.dushyna.ticketflow.cinema.repository.ShowtimeRepository;
import io.github.dushyna.ticketflow.exception.handling.exceptions.common.RestApiException;
import io.github.dushyna.ticketflow.organization.entity.Organization;
import io.github.dushyna.ticketflow.organization.repository.OrganizationRepository;
import io.github.dushyna.ticketflow.user.entity.AppUser;
import io.github.dushyna.ticketflow.user.entity.Role;
import io.github.dushyna.ticketflow.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "EMAIL_USERNAME=test@test.com",
                "EMAIL_PASSWORD=test-password",
                "EMAIL_HOST=localhost",
                "CLIENT_ID=test-id",
                "CLIENT_SECRET=test-secret",
                "JWT_AT_SECRET=dGhpc2lzYXZlcnlzZWN1cmVhY2Nlc3N0b2tlbnNlY3JldGtleTI1Ng==",
                "JWT_RT_SECRET=dGhpc2lzYXZlcnlzZWN1cmVpbmZ1c2lvbnJlZnJlc2h0b2tlbnNlY3JldGtleTI1Ng=="
        }
)
@ActiveProfiles("test")
@Transactional // Rolls back DB changes after each test
class ShowtimeIntegrationTest {

    @Autowired private ShowtimeServiceImpl showtimeService;
    @Autowired private ShowtimeRepository showtimeRepository;
    @Autowired private MovieRepository movieRepository;
    @Autowired private MovieHallRepository hallRepository;
    @Autowired private CinemaRepository cinemaRepository;
    @Autowired private OrganizationRepository organizationRepository;
    @Autowired private UserRepository userRepository;

    private AppUser owner;
    private Movie movie;
    private MovieHall hall;

    @BeforeEach
    void setUp() {
        // 1. Create Organization
        Organization org = Organization.builder()
                .name("Integration Cinema Org")
                .slug("int-cinema-" + Instant.now().toEpochMilli())
                .build();
        org = organizationRepository.save(org);

        // 2. Create Owner
        owner = new AppUser();
        owner.setEmail("owner@integration.com");
        owner.setRole(Role.ROLE_TENANT_ADMIN);
        owner.setOrganization(org);
        owner = userRepository.save(owner);

        // 3. Create Movie (120 min)
        movie = new Movie();
        movie.setTitle("Integration Movie");
        movie.setDurationMinutes(120);
        movie = movieRepository.save(movie);

        // 4. Create Cinema & Hall
        Cinema cinema = new Cinema();
        cinema.setName("Integration Cinema");
        cinema.setOrganization(org);
        cinema = cinemaRepository.save(cinema);

        hall = new MovieHall();
        hall.setName("Main Hall");
        hall.setCinema(cinema);
        hall.setRowsCount(10);
        hall.setColsCount(10);
        hall.setLayoutConfig(Map.of());
        hall = hallRepository.save(hall);
    }

    @Test
    @DisplayName("Should throw CONFLICT when trying to schedule overlapping showtime")
    void shouldThrowConflict_WhenOverlapping() {
        // Given: Tomorrow at 18:00
        Instant tomorrow = Instant.now().plus(Duration.ofDays(1));
        // Round to some clean hour to make it easy
        Instant start1 = tomorrow.truncatedTo(java.time.temporal.ChronoUnit.DAYS).plus(Duration.ofHours(18));

        ShowtimeCreateDto dto1 = new ShowtimeCreateDto(movie.getId(), hall.getId(), start1, BigDecimal.valueOf(100));
        showtimeService.createShowtime(dto1, owner);

        // When: Second showtime starts at 20:00 (CONFLICT! because first ends at 20:15)
        Instant start2 = start1.plus(Duration.ofHours(2));
        ShowtimeCreateDto dto2 = new ShowtimeCreateDto(movie.getId(), hall.getId(), start2, BigDecimal.valueOf(100));

        // Then
        assertThatThrownBy(() -> showtimeService.createShowtime(dto2, owner))
                .isInstanceOf(RestApiException.class)
                .satisfies(ex -> {
                    RestApiException apiEx = (RestApiException) ex;
                    assertThat(apiEx.getHttpStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(apiEx.getMessage()).contains("already booked");
                });
    }

    @Test
    @DisplayName("Should allow showtime exactly after the cleaning buffer")
    void shouldAllow_WhenScheduledAfterBuffer() {
        // Given: Tomorrow at 10:00
        Instant tomorrow = Instant.now().plus(Duration.ofDays(1));
        Instant start1 = tomorrow.truncatedTo(java.time.temporal.ChronoUnit.DAYS).plus(Duration.ofHours(10));

        ShowtimeCreateDto dto1 = new ShowtimeCreateDto(movie.getId(), hall.getId(), start1, BigDecimal.valueOf(100));
        showtimeService.createShowtime(dto1, owner);

        // When: Second showtime starts exactly after buffer (120m movie + 15m buffer = 135m)
        Instant start2 = start1.plus(Duration.ofMinutes(135));
        ShowtimeCreateDto dto2 = new ShowtimeCreateDto(movie.getId(), hall.getId(), start2, BigDecimal.valueOf(100));

        // Then: Should not throw any exception
        var response = showtimeService.createShowtime(dto2, owner);
        assertThat(response).isNotNull();
        assertThat(showtimeRepository.findAllByHallId(hall.getId())).hasSize(2);
    }

    @Test
    @DisplayName("Should successfully update showtime and recalculate end time in DB")
    void shouldUpdateShowtime_AndRecalculateEndTime() {
        // 1. Given: Create initial showtime
        Instant start = Instant.now().plus(Duration.ofDays(1));
        ShowtimeCreateDto createDto = new ShowtimeCreateDto(movie.getId(), hall.getId(), start, BigDecimal.valueOf(100));
        ShowtimeResponseDto initial = showtimeService.createShowtime(createDto, owner);

        // Create a new longer movie (180 min)
        Movie longMovie = new Movie();
        longMovie.setTitle("Long Epic Movie");
        longMovie.setDurationMinutes(180);
        longMovie = movieRepository.save(longMovie);

        // 2. When: Update showtime with new movie
        ShowtimeCreateDto updateDto = new ShowtimeCreateDto(longMovie.getId(), hall.getId(), start, BigDecimal.valueOf(150));
        showtimeService.updateShowtime(initial.id(), updateDto, owner);

        // 3. Then: Verify in DB
        Showtime updated = showtimeRepository.findById(initial.id()).orElseThrow();
        // 180 (movie) + 15 (buffer) = 195 min
        Instant expectedEnd = start.plus(Duration.ofMinutes(195));

        assertThat(updated.getMovie().getId()).isEqualTo(longMovie.getId());
        assertThat(updated.getEndTime()).isEqualTo(expectedEnd);
    }

    @Test
    @DisplayName("Should fetch all showtimes for cinema with preloaded details")
    void shouldReturnShowtimesByCinema_WithDetails() {
        // 1. Given: Create two showtimes in the same cinema
        Instant start1 = Instant.now().plus(Duration.ofDays(1));
        Instant start2 = Instant.now().plus(Duration.ofDays(1)).plus(Duration.ofHours(5));

        showtimeService.createShowtime(new ShowtimeCreateDto(movie.getId(), hall.getId(), start1, BigDecimal.TEN), owner);
        showtimeService.createShowtime(new ShowtimeCreateDto(movie.getId(), hall.getId(), start2, BigDecimal.TEN), owner);

        // 2. When: Fetch by cinema ID
        UUID cinemaId = hall.getCinema().getId();
        List<ShowtimeResponseDto> result = showtimeService.getShowtimesByCinema(cinemaId);

        // 3. Then
        assertThat(result).hasSize(2);
        assertThat(result.getFirst().movieTitle()).isEqualTo(movie.getTitle());
        assertThat(result.getFirst().hallName()).isEqualTo(hall.getName());
    }

    @Test
    @DisplayName("Should remove showtime from database")
    void shouldDeleteShowtime() {
        // 1. Given
        ShowtimeCreateDto dto = new ShowtimeCreateDto(movie.getId(), hall.getId(), Instant.now().plus(Duration.ofDays(1)), BigDecimal.TEN);
        ShowtimeResponseDto created = showtimeService.createShowtime(dto, owner);
        assertThat(showtimeRepository.existsById(created.id())).isTrue();

        // 2. When
        showtimeService.deleteShowtime(created.id(), owner);

        // 3. Then
        assertThat(showtimeRepository.existsById(created.id())).isFalse();
    }

    @Test
    @DisplayName("Edge Case: Should allow showtime starting exactly when previous buffer ends")
    void shouldAllowShowtime_StartingExactlyAtPreviousEndTime() {
        // Given: Showtime 12:00 -> 14:15 (120m + 15m)
        Instant start1 = Instant.now().plus(Duration.ofDays(1)).truncatedTo(java.time.temporal.ChronoUnit.DAYS).plus(Duration.ofHours(12));
        showtimeService.createShowtime(new ShowtimeCreateDto(movie.getId(), hall.getId(), start1, BigDecimal.TEN), owner);

        Instant endTime1 = start1.plus(Duration.ofMinutes(120 + 15));

        // When: New showtime starts exactly at 14:15
        ShowtimeCreateDto dto2 = new ShowtimeCreateDto(movie.getId(), hall.getId(), endTime1, BigDecimal.TEN);

        // Then: Should be successful
        var response = showtimeService.createShowtime(dto2, owner);
        assertThat(response).isNotNull();
    }

    @Test
    @DisplayName("Edge Case: Should throw CONFLICT when new showtime is completely inside existing one")
    void shouldThrowConflict_WhenNewShowtimeIsInsideExisting() {
        // Given: Long showtime 12:00 -> 16:00
        Movie longMovie = new Movie();
        longMovie.setTitle("Titanic");
        longMovie.setDurationMinutes(225); // ~4 hours with buffer
        longMovie = movieRepository.save(longMovie);

        Instant start1 = Instant.now().plus(Duration.ofDays(1)).truncatedTo(java.time.temporal.ChronoUnit.DAYS).plus(Duration.ofHours(12));
        showtimeService.createShowtime(new ShowtimeCreateDto(longMovie.getId(), hall.getId(), start1, BigDecimal.TEN), owner);

        // When: Try to add 13:00 -> 15:00 (inside 12:00-16:00)
        Instant start2 = start1.plus(Duration.ofHours(1));
        ShowtimeCreateDto dto2 = new ShowtimeCreateDto(movie.getId(), hall.getId(), start2, BigDecimal.TEN);

        // Then
        assertThatThrownBy(() -> showtimeService.createShowtime(dto2, owner))
                .isInstanceOf(RestApiException.class)
                .hasFieldOrPropertyWithValue("httpStatus", HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("Edge Case: Should allow updating showtime without changing time (Self-conflict check)")
    void shouldAllowUpdate_WithoutChangingTime() {
        // Given
        Instant start = Instant.now().plus(Duration.ofDays(1));
        ShowtimeResponseDto created = showtimeService.createShowtime(new ShowtimeCreateDto(movie.getId(), hall.getId(), start, BigDecimal.TEN), owner);

        // When: Update price but keep same movie and time
        ShowtimeCreateDto updateDto = new ShowtimeCreateDto(movie.getId(), hall.getId(), start, BigDecimal.valueOf(99.99));

        // Then: Should not conflict with itself
        var updated = showtimeService.updateShowtime(created.id(), updateDto, owner);
        assertThat(updated.basePrice()).isEqualByComparingTo("99.99");
    }

}
