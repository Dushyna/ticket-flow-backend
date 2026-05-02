package io.github.dushyna.ticketflow.cinema.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.dushyna.ticketflow.cinema.dto.request.MovieCreateDto;
import io.github.dushyna.ticketflow.cinema.dto.response.MovieResponseDto;
import io.github.dushyna.ticketflow.cinema.service.interfaces.MovieService;
import io.github.dushyna.ticketflow.security.dto.AuthUserDetails;
import io.github.dushyna.ticketflow.user.entity.AppUser;
import io.github.dushyna.ticketflow.user.entity.Role;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MovieControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MovieService movieService;

    private AuthUserDetails adminDetails;
    private AuthUserDetails regularDetails;

    @BeforeEach
    void setUp() {
        AppUser admin = new AppUser();
        admin.setEmail("admin@cinema.com");
        admin.setRole(Role.ROLE_TENANT_ADMIN);
        adminDetails = new AuthUserDetails(admin);

        AppUser regularUser = new AppUser();
        regularUser.setEmail("user@test.com");
        regularUser.setRole(Role.ROLE_USER);
        regularDetails = new AuthUserDetails(regularUser);
    }

    @Test
    @DisplayName("POST /api/v1/movies - Success: Create movie as TENANT_ADMIN")
    void createMovie_Success() throws Exception {
        // Given
        MovieCreateDto requestDto = new MovieCreateDto("Inception", "Dream thief", 148, "https://url.com", LocalDate.of(2010, 7, 16));
        MovieResponseDto responseDto = new MovieResponseDto(UUID.randomUUID(), "Inception", "Dream thief", 148, "https://url.com", LocalDate.of(2010, 7, 16));

        when(movieService.createMovie(any(MovieCreateDto.class), any(AppUser.class))).thenReturn(responseDto);

        // When & Then
        mockMvc.perform(post("/api/v1/movies")
                        .with(user(adminDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Inception"))
                .andExpect(jsonPath("$.durationMinutes").value(148));
    }

    @Test
    @DisplayName("POST /api/v1/movies - Failure: Validation error (Negative duration)")
    void createMovie_InvalidDuration_ReturnsBadRequest() throws Exception {
        // Given: durationMinutes is 0, violates @Min(1)
        // noinspection ConstantConditions
        MovieCreateDto invalidRequest = new MovieCreateDto("Title", "Desc", 0, "https://url.com", LocalDate.now());

        // When & Then
        mockMvc.perform(post("/api/v1/movies")
                        .with(user(adminDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/movies - Failure: Forbidden for regular USER")
    void createMovie_RegularUser_ReturnsForbidden() throws Exception {
        // Given
        MovieCreateDto requestDto = new MovieCreateDto(
                "Inception", "Dream thief", 148, "https://url.com", LocalDate.now()
        );

        // When & Then: A user with ROLE_USER attempts to create a movie
        mockMvc.perform(post("/api/v1/movies")
                        .with(user(regularDetails)) // Authenticated as ROLE_USER
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/v1/movies - Failure: Unauthorized for Anonymous")
    void createMovie_Anonymous_ReturnsUnauthorized() throws Exception {
        // Given
        MovieCreateDto requestDto = new MovieCreateDto("Title", "Desc", 120, "https://url.com", LocalDate.now());

        // When & Then: Request without any authentication context
        mockMvc.perform(post("/api/v1/movies")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/v1/movies - Failure: Validation error (Blank Title)")
    void createMovie_BlankTitle_ReturnsBadRequest() throws Exception {
        // Given: title is blank, violates @NotBlank
        MovieCreateDto invalidRequest = new MovieCreateDto("", "Description", 120, "https://url.com", LocalDate.now());

        // When & Then
        mockMvc.perform(post("/api/v1/movies")
                        .with(user(adminDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").exists());
    }


    @Test
    @DisplayName("POST /api/v1/movies - Failure: Conflict (Movie already exists)")
    void createMovie_DuplicateTitle_ReturnsConflict() throws Exception {
        // Given
        MovieCreateDto requestDto = new MovieCreateDto("Existing Movie", "Desc", 100, "https://url.com", LocalDate.now());

        // Mock service to throw 409 Conflict (custom exception)
        when(movieService.createMovie(any(MovieCreateDto.class), any()))
                .thenThrow(new io.github.dushyna.ticketflow.exception.handling.exceptions.common.RestApiException(
                        org.springframework.http.HttpStatus.CONFLICT, "Movie with this title already exists"));

        // When & Then
        mockMvc.perform(post("/api/v1/movies")
                        .with(user(adminDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("PUT /api/v1/movies/{id} - Success: Full update")
    void updateMovie_Success() throws Exception {
        // Given
        UUID movieId = UUID.randomUUID();
        MovieCreateDto updateDto = new MovieCreateDto("New Title", "New Desc", 120, "https://new.com", LocalDate.now());
        MovieResponseDto responseDto = new MovieResponseDto(movieId, "New Title", "New Desc", 120, "https://new.com", LocalDate.now());

        when(movieService.updateMovie(eq(movieId), any(MovieCreateDto.class), any(AppUser.class))).thenReturn(responseDto);

        // When & Then
        mockMvc.perform(put("/api/v1/movies/{id}", movieId)
                        .with(user(adminDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("New Title"));
    }

    @Test
    @DisplayName("PUT /api/v1/movies/{id} - Failure: Forbidden for regular USER")
    void updateMovie_RegularUser_ReturnsForbidden() throws Exception {
        // Given
        UUID movieId = UUID.randomUUID();
        MovieCreateDto updateDto = new MovieCreateDto("New Title", "Desc", 120, "https://url.com", LocalDate.now());

        // When & Then: ROLE_USER attempts to update
        mockMvc.perform(put("/api/v1/movies/{id}", movieId)
                        .with(user(regularDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /api/v1/movies/{id} - Failure: Movie Not Found")
    void updateMovie_NotFound_Returns404() throws Exception {
        // Given
        UUID missingId = UUID.randomUUID();
        MovieCreateDto updateDto = new MovieCreateDto("Title", "Desc", 120, "https://url.com", LocalDate.now());

        // Mocking service to throw EntityNotFoundException (handled by GlobalExceptionHandler)
        when(movieService.updateMovie(eq(missingId), any(MovieCreateDto.class), any()))
                .thenThrow(new EntityNotFoundException("Movie not found with id: " + missingId));

        // When & Then
        mockMvc.perform(put("/api/v1/movies/{id}", missingId)
                        .with(user(adminDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /api/v1/movies/{id} - Failure: Validation error (Negative duration)")
    void updateMovie_InvalidData_ReturnsBadRequest() throws Exception {
        // Given: duration is -5, violates @Min(1)
        UUID movieId = UUID.randomUUID();
        // noinspection ConstantConditions
        MovieCreateDto invalidDto = new MovieCreateDto("Valid Title", "Desc", -5, "https://url.com", LocalDate.now());

        // When & Then
        mockMvc.perform(put("/api/v1/movies/{id}", movieId)
                        .with(user(adminDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /api/v1/movies/{id} - Edge Case: Malformed UUID")
    void updateMovie_MalformedUuid_ReturnsBadRequest() throws Exception {
        // Given
        MovieCreateDto updateDto = new MovieCreateDto("Title", "Desc", 100, "https://url.com", LocalDate.now());

        // When & Then: Providing a string that is not a UUID format
        mockMvc.perform(put("/api/v1/movies/{id}", "invalid-uuid-format")
                        .with(user(adminDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /api/v1/movies/{id} - Failure: Empty Request Body")
    void updateMovie_EmptyBody_ReturnsBadRequest() throws Exception {
        // Given
        UUID movieId = UUID.randomUUID();

        // When & Then: No JSON content provided
        mockMvc.perform(put("/api/v1/movies/{id}", movieId)
                        .with(user(adminDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/movies - Success: Publicly accessible")
    void getAllMovies_PublicAccess() throws Exception {
        // Given
        MovieResponseDto movie = new MovieResponseDto(UUID.randomUUID(), "Avatar", "Blue people", 162, "url", LocalDate.now());
        when(movieService.getAllMovies()).thenReturn(List.of(movie));

        // When & Then
        mockMvc.perform(get("/api/v1/movies")) // No user needed
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Avatar"));
    }

    @Test
    @DisplayName("GET /api/v1/movies - Success: Empty list returns empty array (Edge Case)")
    void getAllMovies_EmptyDatabase_ReturnsEmptyArray() throws Exception {
        // Given: Catalog is empty
        when(movieService.getAllMovies()).thenReturn(List.of());

        // When & Then: Should return 200 OK and an empty JSON array []
        mockMvc.perform(get("/api/v1/movies")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0))
                .andExpect(content().string("[]"));
    }

    @Test
    @DisplayName("GET /api/v1/movies - Failure: Service Layer Exception (Edge Case)")
    void getAllMovies_ServiceError_ReturnsInternalServerError() throws Exception {
        // Given: Service fails unexpectedly (e.g., database timeout)
        when(movieService.getAllMovies())
                .thenThrow(new RuntimeException("Database is down"));

        // When & Then: GlobalExceptionHandler should map this to 500
        mockMvc.perform(get("/api/v1/movies")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500));
    }

    @Test
    @DisplayName("GET /api/v1/movies - Success: Large Catalog performance (Edge Case)")
    void getAllMovies_LargeList_ReturnsAllElements() throws Exception {
        // Given: Simulate a large number of movies
        List<MovieResponseDto> manyMovies = new java.util.ArrayList<>();
        for (int i = 0; i < 50; i++) {
            manyMovies.add(new MovieResponseDto(UUID.randomUUID(), "Movie " + i, "Desc", 100, "https://url.com", LocalDate.now()));
        }
        when(movieService.getAllMovies()).thenReturn(manyMovies);

        // When & Then
        mockMvc.perform(get("/api/v1/movies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(50));
    }

    @Test
    @DisplayName("GET /api/v1/movies - Failure: Unauthorized if SecurityConfig is changed (Edge Case)")
    void getAllMovies_Anonymous_ReturnsUnauthorized_IfProtected() throws Exception {
        // Note: This test checks if SecurityFilterChain might override @PreAuthorize
        // If you accidentally remove permitAll from SecurityConfig, this will detect it.
        // Assuming it's public for now, but good to keep in mind.

        mockMvc.perform(get("/api/v1/movies"))
                .andDo(print())
                // Currently, we expect 200 because of permitAll()
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/movies/{id} - Failure: Movie Not Found")
    void getMovieById_NotFound_Returns404() throws Exception {
        // Given
        UUID missingId = UUID.randomUUID();
        when(movieService.getMovieById(missingId)).thenThrow(new EntityNotFoundException("Movie not found"));

        // When & Then
        mockMvc.perform(get("/api/v1/movies/{id}", missingId))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/movies/{id} - Success: Returns movie details")
    void getMovieById_Success() throws Exception {
        // Given
        UUID movieId = UUID.randomUUID();
        MovieResponseDto responseDto = new MovieResponseDto(
                movieId,
                "Interstellar",
                "Space exploration",
                169,
                "https://images.com",
                LocalDate.of(2014, 11, 7)
        );

        when(movieService.getMovieById(movieId)).thenReturn(responseDto);

        // When & Then
        mockMvc.perform(get("/api/v1/movies/{id}", movieId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(movieId.toString()))
                .andExpect(jsonPath("$.title").value("Interstellar"))
                .andExpect(jsonPath("$.durationMinutes").value(169));
    }

    @Test
    @DisplayName("GET /api/v1/movies/{id} - Success: Accessible by authorized user")
    void getMovieById_WithAuth_ReturnsOk() throws Exception {
        // Given
        UUID movieId = UUID.randomUUID();
        MovieResponseDto responseDto = new MovieResponseDto(movieId, "Title", "Desc", 100, null, LocalDate.now());

        when(movieService.getMovieById(movieId)).thenReturn(responseDto);

        // When & Then: Verifying that even with a token, the endpoint remains accessible
        mockMvc.perform(get("/api/v1/movies/{id}", movieId)
                        .with(user(regularDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Title"));
    }

    @Test
    @DisplayName("GET /api/v1/movies/{id} - Failure: Malformed UUID string")
    void getMovieById_InvalidUuidFormat_ReturnsBadRequest() throws Exception {
        // When & Then: Passing 'abc-123' instead of a valid UUID format
        mockMvc.perform(get("/api/v1/movies/{id}", "abc-123"))
                .andDo(print())
                .andExpect(status().isBadRequest())
                // Verify that GlobalExceptionHandler handles MethodArgumentTypeMismatchException
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("GET /api/v1/movies/{id} - Failure: Data not found in DB")
    void getMovieById_ValidUuidButNoData_Returns404() throws Exception {
        // 1. Given: A perfectly valid UUID
        UUID validId = UUID.randomUUID();

        // 2. Mock service to throw EntityNotFoundException
        // Ensure you import jakarta.persistence.EntityNotFoundException
        when(movieService.getMovieById(validId))
                .thenThrow(new jakarta.persistence.EntityNotFoundException("Movie not found"));

        // 3. When & Then: Should return 404 because GlobalExceptionHandler catches it
        mockMvc.perform(get("/api/v1/movies/{id}", validId))
                .andDo(print())
                .andExpect(status().isNotFound());
    }


    @Test
    @DisplayName("DELETE /api/v1/movies/{id} - Success: Deleted by TENANT_ADMIN")
    void deleteMovie_Success() throws Exception {
        // Given
        UUID movieId = UUID.randomUUID();

        // Mocking service to perform deletion without errors
        doNothing().when(movieService).deleteMovie(eq(movieId), any(AppUser.class));

        // When & Then
        mockMvc.perform(delete("/api/v1/movies/{id}", movieId)
                        .with(user(adminDetails)) // Authenticated as TENANT_ADMIN
                .with(csrf()))
                .andDo(print())
                .andExpect(status().isNoContent()); // Verifying 204 status
    }

    @Test
    @DisplayName("DELETE /api/v1/movies/{id} - Failure: Movie Not Found")
    void deleteMovie_NotFound_Returns404() throws Exception {
        // Given
        UUID missingId = UUID.randomUUID();

        // Simulating EntityNotFoundException when calling delete on non-existent ID
        doThrow(new jakarta.persistence.EntityNotFoundException("Movie not found"))
                .when(movieService).deleteMovie(eq(missingId), any(AppUser.class));

        // When & Then
        mockMvc.perform(delete("/api/v1/movies/{id}", missingId)
                        .with(user(adminDetails))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/v1/movies/{id} - Failure: Unauthorized for Anonymous")
    void deleteMovie_Anonymous_ReturnsUnauthorized() throws Exception {
        // Given
        UUID movieId = UUID.randomUUID();

        // When & Then: Request without any auth context
        mockMvc.perform(delete("/api/v1/movies/{id}", movieId)
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("DELETE /api/v1/movies/{id} - Edge Case: Malformed UUID format")
    void deleteMovie_InvalidUuid_ReturnsBadRequest() throws Exception {
        // When & Then: Providing a string that is not a valid UUID format
        mockMvc.perform(delete("/api/v1/movies/{id}", "not-a-uuid-string")
                        .with(user(adminDetails))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }


    @Test
    @DisplayName("DELETE /api/v1/movies/{id} - Failure: Forbidden for regular USER")
    void deleteMovie_RegularUser_ReturnsForbidden() throws Exception {
        // Given
        UUID movieId = UUID.randomUUID();

        // When & Then
        mockMvc.perform(delete("/api/v1/movies/{id}", movieId)
                        .with(user(regularDetails))
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

}
