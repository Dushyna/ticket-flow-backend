package io.github.dushyna.ticketflow.cinema.service.impl;

import io.github.dushyna.ticketflow.cinema.dto.request.MovieHallCreateDto;
import io.github.dushyna.ticketflow.cinema.dto.response.MovieHallResponseDto;
import io.github.dushyna.ticketflow.cinema.entity.Cinema;
import io.github.dushyna.ticketflow.cinema.entity.MovieHall;
import io.github.dushyna.ticketflow.cinema.repository.CinemaRepository;
import io.github.dushyna.ticketflow.cinema.repository.MovieHallRepository;
import io.github.dushyna.ticketflow.cinema.utils.MovieHallMapper;
import io.github.dushyna.ticketflow.organization.entity.Organization;
import io.github.dushyna.ticketflow.user.entity.AppUser;
import io.github.dushyna.ticketflow.user.entity.Role;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit tests for MovieHallServiceImpl")
class MovieHallServiceImplTest {

    @Mock private MovieHallRepository movieHallRepository;
    @Mock private CinemaRepository cinemaRepository;
    @Mock private MovieHallMapper mappingService;

    @InjectMocks
    private MovieHallServiceImpl movieHallService;

    private AppUser tenantAdmin;
    private Cinema myCinema;

    @BeforeEach
    void setUp() {
        Organization myOrg = new Organization();
        ReflectionTestUtils.setField(myOrg, "id", UUID.randomUUID());

        tenantAdmin = new AppUser();
        tenantAdmin.setRole(Role.ROLE_TENANT_ADMIN);
        tenantAdmin.setOrganization(myOrg);

        myCinema = new Cinema();
        myCinema.setOrganization(myOrg);
        ReflectionTestUtils.setField(myCinema, "id", UUID.randomUUID());
    }

    @Test
    @DisplayName("createHall: Success - admin should create hall in their cinema")
    void createHall_Success() {
        // Given
        UUID cinemaId = myCinema.getId();
        MovieHallCreateDto dto = new MovieHallCreateDto("IMAX", cinemaId, 10, 10, Map.of("version", 1));
        MovieHall hall = new MovieHall();

        given(cinemaRepository.findById(cinemaId)).willReturn(Optional.of(myCinema));
        given(mappingService.mapDtoToEntity(dto)).willReturn(hall);
        given(movieHallRepository.save(any(MovieHall.class))).willReturn(hall);
        given(mappingService.mapEntityToResponseDto(any())).willReturn(mock(MovieHallResponseDto.class));

        // When
        movieHallService.createHall(dto, tenantAdmin);

        // Then
        assertThat(hall.getCinema()).isEqualTo(myCinema);
        verify(movieHallRepository).save(hall);
    }

    @Test
    @DisplayName("createHall: Failure - Cinema not found")
    void createHall_CinemaNotFound_ThrowsException() {
        // Given
        UUID cinemaId = UUID.randomUUID();
        MovieHallCreateDto dto = new MovieHallCreateDto("Hall", cinemaId, 5, 5, Map.of());
        given(cinemaRepository.findById(cinemaId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> movieHallService.createHall(dto, tenantAdmin))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Cinema not found");
    }

    @Test
    @DisplayName("createHall: Failure - Forbidden to create hall in another organization's cinema")
    void createHall_Forbidden_ThrowsException() {
        // 1. Given
        UUID otherCinemaId = UUID.randomUUID();
        MovieHallCreateDto dto = new MovieHallCreateDto("Hacker Hall", otherCinemaId, 10, 10, Map.of());

        Organization otherOrg = new Organization();
        ReflectionTestUtils.setField(otherOrg, "id", UUID.randomUUID()); // Інший ID організації

        Cinema otherCinema = new Cinema();
        otherCinema.setOrganization(otherOrg);
        ReflectionTestUtils.setField(otherCinema, "id", otherCinemaId);

        given(cinemaRepository.findById(otherCinemaId)).willReturn(Optional.of(otherCinema));

        // 2. When & Then
        assertThatThrownBy(() -> movieHallService.createHall(dto, tenantAdmin))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Forbidden: You don't have access");

        verify(movieHallRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateHall: Success - should update hall with access validation")
    void updateHall_Success() {
        // Given
        UUID id = UUID.randomUUID();
        MovieHallCreateDto dto = new MovieHallCreateDto("New Name", myCinema.getId(), 8, 8, Map.of());
        MovieHall existingHall = new MovieHall();
        existingHall.setCinema(myCinema);

        given(movieHallRepository.findById(id)).willReturn(Optional.of(existingHall));
        given(movieHallRepository.save(any(MovieHall.class))).willReturn(existingHall);
        given(mappingService.mapEntityToResponseDto(any())).willReturn(mock(MovieHallResponseDto.class));

        // When
        movieHallService.updateHall(id, dto, tenantAdmin);

        // Then
        verify(mappingService).updateEntityFromDto(dto, existingHall);
        verify(movieHallRepository).save(existingHall);
    }

    @Test
    @DisplayName("updateHall: Failure - Forbidden to update hall of another organization")
    void updateHall_Forbidden_ThrowsException() {
        // 1. Given
        UUID hallId = UUID.randomUUID();
        MovieHallCreateDto dto = new MovieHallCreateDto("Renamed Hall", myCinema.getId(), 5, 5, Map.of());

        Organization otherOrg = new Organization();
        ReflectionTestUtils.setField(otherOrg, "id", UUID.randomUUID());

        Cinema otherCinema = new Cinema();
        otherCinema.setOrganization(otherOrg);

        MovieHall otherHall = new MovieHall();
        otherHall.setCinema(otherCinema);

        given(movieHallRepository.findById(hallId)).willReturn(Optional.of(otherHall));

        // 2. When & Then
        assertThatThrownBy(() -> movieHallService.updateHall(hallId, dto, tenantAdmin))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Forbidden");

        verify(movieHallRepository, never()).save(any());
    }

    @Test
    @DisplayName("getAllByCinema: Success - Tenant Admin can see halls of their cinema")
    void getAllByCinema_TenantAdmin_Success() {
        // Given
        UUID cinemaId = myCinema.getId();
        MovieHall hall = new MovieHall();

        given(cinemaRepository.findById(cinemaId)).willReturn(Optional.of(myCinema));
        given(movieHallRepository.findAllByCinemaId(cinemaId)).willReturn(List.of(hall));
        given(mappingService.mapEntityToResponseDto(any())).willReturn(mock(MovieHallResponseDto.class));

        // When
        List<MovieHallResponseDto> result = movieHallService.getAllByCinema(cinemaId, tenantAdmin);

        // Then
        assertThat(result).hasSize(1);
        verify(cinemaRepository).findById(cinemaId);
        verify(movieHallRepository).findAllByCinemaId(cinemaId);
    }

    @Test
    @DisplayName("getAllByCinema: Success - Regular user can see any cinema halls without validation")
    void getAllByCinema_RegularUser_Success() {
        // Given
        UUID cinemaId = UUID.randomUUID();
        AppUser regularUser = new AppUser();
        regularUser.setRole(Role.ROLE_USER);

        given(movieHallRepository.findAllByCinemaId(cinemaId)).willReturn(List.of(new MovieHall()));
        given(mappingService.mapEntityToResponseDto(any())).willReturn(mock(MovieHallResponseDto.class));

        // When
        List<MovieHallResponseDto> result = movieHallService.getAllByCinema(cinemaId, regularUser);

        // Then
        assertThat(result).hasSize(1);
        verify(cinemaRepository, never()).findById(any()); // Access validation skipped
    }

    @Test
    @DisplayName("getAllByCinema: Failure - Cinema not found for Tenant Admin")
    void getAllByCinema_NotFound_ThrowsException() {
        // Given
        UUID cinemaId = UUID.randomUUID();
        given(cinemaRepository.findById(cinemaId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> movieHallService.getAllByCinema(cinemaId, tenantAdmin))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Cinema not found");
    }

    @Test
    @DisplayName("getByIdOrThrow: Success - Tenant Admin can view own hall")
    void getByIdOrThrow_TenantAdmin_Success() {
        // Given
        UUID hallId = UUID.randomUUID();
        MovieHall hall = new MovieHall();
        hall.setCinema(myCinema);

        given(movieHallRepository.findById(hallId)).willReturn(Optional.of(hall));
        given(mappingService.mapEntityToResponseDto(hall)).willReturn(mock(MovieHallResponseDto.class));

        // When
        MovieHallResponseDto result = movieHallService.getByIdOrThrow(hallId, tenantAdmin);

        // Then
        assertThat(result).isNotNull();
        verify(movieHallRepository).findById(hallId);
    }

    @Test
    @DisplayName("getByIdOrThrow: Success - Anonymous or Regular user can view any hall")
    void getByIdOrThrow_PublicAccess_Success() {
        // Given
        UUID hallId = UUID.randomUUID();
        MovieHall hall = new MovieHall();
        hall.setCinema(new Cinema()); // Hall from another cinema/org

        AppUser regularUser = new AppUser();
        regularUser.setRole(Role.ROLE_USER);

        given(movieHallRepository.findById(hallId)).willReturn(Optional.of(hall));
        given(mappingService.mapEntityToResponseDto(hall)).willReturn(mock(MovieHallResponseDto.class));

        // When
        MovieHallResponseDto result = movieHallService.getByIdOrThrow(hallId, regularUser);

        // Then
        assertThat(result).isNotNull();
        // validateAccess was skipped because role is not ROLE_TENANT_ADMIN
    }

    @Test
    @DisplayName("getByIdOrThrow: Failure - Tenant Admin cannot view hall from another organization")
    void getByIdOrThrow_TenantAdmin_Forbidden() {
        // Given
        UUID hallId = UUID.randomUUID();

        Organization otherOrg = new Organization();
        ReflectionTestUtils.setField(otherOrg, "id", UUID.randomUUID());

        Cinema otherCinema = new Cinema();
        otherCinema.setOrganization(otherOrg);

        MovieHall otherHall = new MovieHall();
        otherHall.setCinema(otherCinema);

        given(movieHallRepository.findById(hallId)).willReturn(Optional.of(otherHall));

        // When & Then
        assertThatThrownBy(() -> movieHallService.getByIdOrThrow(hallId, tenantAdmin))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Forbidden");
    }

    @Test
    @DisplayName("getByIdOrThrow: Success for SuperAdmin with bypass")
    void getByIdOrThrow_SuperAdmin_Success() {
        // Given
        UUID id = UUID.randomUUID();
        AppUser superAdmin = new AppUser();
        superAdmin.setRole(Role.ROLE_SUPER_ADMIN);

        MovieHall hall = new MovieHall();
        hall.setCinema(new Cinema()); // Different cinema/org

        given(movieHallRepository.findById(id)).willReturn(Optional.of(hall));
        given(mappingService.mapEntityToResponseDto(hall)).willReturn(mock(MovieHallResponseDto.class));

        // When
        movieHallService.getByIdOrThrow(id, superAdmin);

        // Then
        verify(mappingService).mapEntityToResponseDto(hall);
        // validateAccess was bypassed
    }

    @Test
    @DisplayName("getByIdOrThrow: Edge Case - should throw exception when hall id does not exist")
    void getByIdOrThrow_NotFound_ThrowsException() {
        // Given
        UUID fakeId = UUID.randomUUID();
        given(movieHallRepository.findById(fakeId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> movieHallService.getByIdOrThrow(fakeId, tenantAdmin))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Hall not found with id");
    }

    @Test
    @DisplayName("deleteHall: Failure - Forbidden for different organization")
    void deleteHall_Forbidden_ThrowsException() {
        // Given
        UUID id = UUID.randomUUID();
        Organization otherOrg = new Organization();
        ReflectionTestUtils.setField(otherOrg, "id", UUID.randomUUID());

        Cinema otherCinema = new Cinema();
        otherCinema.setOrganization(otherOrg);

        MovieHall hall = new MovieHall();
        hall.setCinema(otherCinema);

        given(movieHallRepository.findById(id)).willReturn(Optional.of(hall));

        // When & Then
        assertThatThrownBy(() -> movieHallService.deleteHall(id, tenantAdmin))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Forbidden");
    }

    @Test
    @DisplayName("deleteHall: Success - owner should be able to delete their hall")
    void deleteHall_Success() {
        // Given
        UUID hallId = UUID.randomUUID();
        MovieHall hall = new MovieHall();
        hall.setCinema(myCinema);

        given(movieHallRepository.findById(hallId)).willReturn(Optional.of(hall));

        // When
        movieHallService.deleteHall(hallId, tenantAdmin);

        // Then
        verify(movieHallRepository).delete(hall);
    }

    @Test
    @DisplayName("deleteHall: Failure - should throw exception when hall not found")
    void deleteHall_NotFound_ThrowsException() {
        // Given
        UUID fakeId = UUID.randomUUID();
        given(movieHallRepository.findById(fakeId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> movieHallService.deleteHall(fakeId, tenantAdmin))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Hall not found with id");

        verify(movieHallRepository, never()).delete(any());
    }

}
