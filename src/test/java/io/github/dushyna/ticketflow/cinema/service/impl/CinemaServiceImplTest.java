package io.github.dushyna.ticketflow.cinema.service.impl;

import io.github.dushyna.ticketflow.cinema.dto.request.CinemaCreateDto;
import io.github.dushyna.ticketflow.cinema.dto.response.CinemaResponseDto;
import io.github.dushyna.ticketflow.cinema.entity.Cinema;
import io.github.dushyna.ticketflow.cinema.repository.CinemaRepository;
import io.github.dushyna.ticketflow.cinema.utils.CinemaMapper;
import io.github.dushyna.ticketflow.organization.entity.Organization;
import io.github.dushyna.ticketflow.user.entity.AppUser;
import io.github.dushyna.ticketflow.user.entity.Role;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit tests for CinemaServiceImpl")
class CinemaServiceImplTest {

    @Mock private CinemaRepository cinemaRepository;
    @Mock private CinemaMapper mappingService;

    @InjectMocks
    private CinemaServiceImpl cinemaService;

    private AppUser adminUser;
    private Organization organization;

    @BeforeEach
    void setUp() {
        organization = new Organization();
        ReflectionTestUtils.setField(organization, "id", UUID.randomUUID());

        adminUser = new AppUser();
        adminUser.setRole(Role.ROLE_TENANT_ADMIN);
        adminUser.setOrganization(organization);
    }

    @Test
    @DisplayName("createCinema: Success - owner should create cinema")
    void createCinema_Success() {
        // Given
        CinemaCreateDto dto = new CinemaCreateDto("Cinema Star", "123 Street");
        Cinema cinema = new Cinema();
        cinema.setName("Cinema Star");

        given(mappingService.mapDtoToEntity(dto)).willReturn(cinema);
        given(cinemaRepository.save(any(Cinema.class))).willReturn(cinema);
        given(mappingService.mapEntityToResponseDto(cinema)).willReturn(mock(CinemaResponseDto.class));

        // When
        cinemaService.createCinema(dto, adminUser);

        // Then
        assertThat(cinema.getOrganization()).isEqualTo(organization);
        verify(cinemaRepository).save(cinema);
    }

    @Test
    @DisplayName("createCinema: Failure - User without organization")
    void createCinema_NoOrganization_ThrowsException() {
        // Given
        adminUser.setOrganization(null);
        CinemaCreateDto dto = new CinemaCreateDto("Bad Cinema", "Nowhere");

        // When & Then
        assertThatThrownBy(() -> cinemaService.createCinema(dto, adminUser))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("User must belong to an organization");
    }


    @Test
    @DisplayName("createCinema: Edge Case - SuperAdmin without organization should also fail")
    void createCinema_SuperAdminNoOrg_ThrowsException() {
        // Given
        AppUser superAdmin = new AppUser();
        superAdmin.setRole(Role.ROLE_SUPER_ADMIN);
        superAdmin.setOrganization(null);

        CinemaCreateDto dto = new CinemaCreateDto("Central Cinema", "City Center");

        // When & Then
        assertThatThrownBy(() -> cinemaService.createCinema(dto, superAdmin))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("User must belong to an organization");
    }

    @Test
    @DisplayName("createCinema: Edge Case - Very long name or address")
    void createCinema_ExtremeInputLengths_Success() {
        // Given
        String longName = "A".repeat(255);
        CinemaCreateDto dto = new CinemaCreateDto(longName, "Some address");

        Cinema cinema = new Cinema();
        cinema.setName(longName);

        given(mappingService.mapDtoToEntity(dto)).willReturn(cinema);
        given(cinemaRepository.save(any(Cinema.class))).willReturn(cinema);
        given(mappingService.mapEntityToResponseDto(any())).willReturn(mock(CinemaResponseDto.class));

        // When
        CinemaResponseDto result = cinemaService.createCinema(dto, adminUser);

        // Then
        assertThat(result).isNotNull();
        verify(cinemaRepository).save(argThat(c -> c.getName().length() == 255));
    }

    @Test
    @DisplayName("updateCinema: Success - owner should update their cinema")
    void updateCinema_Success() {
        // 1. Given
        UUID cinemaId = UUID.randomUUID();
        CinemaCreateDto updateDto = new CinemaCreateDto("Updated Name", "New Address");

        Cinema existingCinema = new Cinema();
        existingCinema.setOrganization(organization); // Наша організація з setUp()

        given(cinemaRepository.findById(cinemaId)).willReturn(Optional.of(existingCinema));
        given(cinemaRepository.save(any(Cinema.class))).willReturn(existingCinema);
        given(mappingService.mapEntityToResponseDto(any())).willReturn(mock(CinemaResponseDto.class));

        // 2. When
        cinemaService.updateCinema(cinemaId, updateDto, adminUser);

        // 3. Then
        verify(mappingService).updateEntityFromDto(updateDto, existingCinema);
        verify(cinemaRepository).save(existingCinema);
    }

    @Test
    @DisplayName("updateCinema: Failure - Cinema not found")
    void updateCinema_NotFound_ThrowsRestApiException() {
        // Given
        UUID cinemaId = UUID.randomUUID();
        given(cinemaRepository.findById(cinemaId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> cinemaService.updateCinema(cinemaId, mock(CinemaCreateDto.class), adminUser))
                .isInstanceOf(io.github.dushyna.ticketflow.exception.handling.exceptions.common.RestApiException.class)
                .hasFieldOrPropertyWithValue("httpStatus", org.springframework.http.HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("updateCinema: Edge Case - SuperAdmin can update any cinema")
    void updateCinema_SuperAdmin_Success() {
        // 1. Given
        UUID cinemaId = UUID.randomUUID();
        AppUser superAdmin = new AppUser();
        superAdmin.setRole(Role.ROLE_SUPER_ADMIN);

        Organization otherOrg = new Organization();
        ReflectionTestUtils.setField(otherOrg, "id", UUID.randomUUID());
        Cinema cinema = new Cinema();
        cinema.setOrganization(otherOrg);

        given(cinemaRepository.findById(cinemaId)).willReturn(Optional.of(cinema));
        given(cinemaRepository.save(any(Cinema.class))).willReturn(cinema);
        given(mappingService.mapEntityToResponseDto(any())).willReturn(mock(CinemaResponseDto.class));

        // 2. When
        cinemaService.updateCinema(cinemaId, new CinemaCreateDto("Admin Overwrite", "Address"), superAdmin);

        // 3. Then
        verify(cinemaRepository).save(cinema);
    }

    @Test
    @DisplayName("getAllForUser: User or Anonymous - should return all cinemas")
    void getAllForUser_UserRole_ReturnsAll() {
        // Given
        AppUser user = new AppUser();
        user.setRole(Role.ROLE_USER);
        Cinema cinema = new Cinema();

        given(cinemaRepository.findAll()).willReturn(List.of(cinema));
        given(mappingService.mapEntityToResponseDto(any())).willReturn(mock(CinemaResponseDto.class));

        // When
        List<CinemaResponseDto> result = cinemaService.getAllForUser(user);

        // Then
        assertThat(result).hasSize(1);
        verify(cinemaRepository).findAll();
        verify(cinemaRepository, never()).findAllByOrganizationId(any());
    }

    @Test
    @DisplayName("getAllForUser: Super Admin - should return all cinemas")
    void getAllForUser_SuperAdmin_ReturnsAll() {
        // Given
        AppUser superAdmin = new AppUser();
        superAdmin.setRole(Role.ROLE_SUPER_ADMIN);

        given(cinemaRepository.findAll()).willReturn(List.of(new Cinema(), new Cinema()));
        given(mappingService.mapEntityToResponseDto(any())).willReturn(mock(CinemaResponseDto.class));

        // When
        List<CinemaResponseDto> result = cinemaService.getAllForUser(superAdmin);

        // Then
        assertThat(result).hasSize(2);
        verify(cinemaRepository).findAll();
    }

    @Test
    @DisplayName("getAllForUser: Tenant Admin - should return only organization cinemas")
    void getAllForUser_TenantAdmin_ReturnsOnlyOwn() {
        // Given
        UUID orgId = organization.getId();
        adminUser.setRole(Role.ROLE_TENANT_ADMIN);

        given(cinemaRepository.findAllByOrganizationId(orgId)).willReturn(List.of(new Cinema()));
        given(mappingService.mapEntityToResponseDto(any())).willReturn(mock(CinemaResponseDto.class));

        // When
        List<CinemaResponseDto> result = cinemaService.getAllForUser(adminUser);

        // Then
        assertThat(result).hasSize(1);
        verify(cinemaRepository).findAllByOrganizationId(orgId);
        verify(cinemaRepository, never()).findAll();
    }

    @Test
    @DisplayName("getAllForUser: Edge Case - returns empty list when no cinemas found")
    void getAllForUser_EmptyResult_ReturnsEmptyList() {
        // Given
        given(cinemaRepository.findAll()).willReturn(List.of());

        // When
        List<CinemaResponseDto> result = cinemaService.getAllForUser(null); // Anonymous case

        // Then
        assertThat(result).isEmpty();
        verify(cinemaRepository).findAll();
    }

    @Test
    @DisplayName("getByIdOrThrow: Success - User or Anonymous can view any cinema")
    void getByIdOrThrow_PublicAccess_Success() {
        // Given
        UUID id = UUID.randomUUID();
        Cinema cinema = new Cinema();
        AppUser viewer = new AppUser();
        viewer.setRole(Role.ROLE_USER);

        given(cinemaRepository.findById(id)).willReturn(Optional.of(cinema));
        given(mappingService.mapEntityToResponseDto(cinema)).willReturn(mock(CinemaResponseDto.class));

        // When
        CinemaResponseDto result = cinemaService.getByIdOrThrow(id, viewer);

        // Then
        assertThat(result).isNotNull();
        verify(cinemaRepository).findById(id);
    }

    @Test
    @DisplayName("getByIdOrThrow: Success - Tenant Admin can view own cinema")
    void getByIdOrThrow_TenantAdmin_Success() {
        // Given
        UUID id = UUID.randomUUID();
        Cinema cinema = new Cinema();
        cinema.setOrganization(organization);

        given(cinemaRepository.findById(id)).willReturn(Optional.of(cinema));
        given(mappingService.mapEntityToResponseDto(cinema)).willReturn(mock(CinemaResponseDto.class));

        // When
        CinemaResponseDto result = cinemaService.getByIdOrThrow(id, adminUser);

        // Then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("getByIdOrThrow: Failure - Tenant Admin cannot view other's cinema")
    void getByIdOrThrow_TenantAdmin_Forbidden() {
        // Given
        UUID id = UUID.randomUUID();
        Organization otherOrg = new Organization();
        ReflectionTestUtils.setField(otherOrg, "id", UUID.randomUUID());

        Cinema cinema = new Cinema();
        cinema.setOrganization(otherOrg);

        given(cinemaRepository.findById(id)).willReturn(Optional.of(cinema));

        // When & Then
        assertThatThrownBy(() -> cinemaService.getByIdOrThrow(id, adminUser))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Forbidden");
    }

    @Test
    @DisplayName("getByIdOrThrow: Edge Case - Super Admin can view any cinema")
    void getByIdOrThrow_SuperAdmin_Success() {
        // Given
        UUID id = UUID.randomUUID();
        AppUser superAdmin = new AppUser();
        superAdmin.setRole(Role.ROLE_SUPER_ADMIN);

        Cinema cinema = new Cinema();
        cinema.setOrganization(new Organization()); // any other org

        given(cinemaRepository.findById(id)).willReturn(Optional.of(cinema));
        given(mappingService.mapEntityToResponseDto(cinema)).willReturn(mock(CinemaResponseDto.class));

        // When
        CinemaResponseDto result = cinemaService.getByIdOrThrow(id, superAdmin);

        // Then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("deleteCinema: Success - owner should be able to delete their cinema")
    void deleteCinema_Success() {
        // Given
        UUID cinemaId = UUID.randomUUID();
        Cinema cinema = new Cinema();
        cinema.setOrganization(organization);

        given(cinemaRepository.findById(cinemaId)).willReturn(Optional.of(cinema));

        // When
        cinemaService.deleteCinema(cinemaId, adminUser);

        // Then
        verify(cinemaRepository).delete(cinema);
    }

    @Test
    @DisplayName("deleteCinema: Failure - Cinema not found")
    void deleteCinema_NotFound_ThrowsException() {
        // Given
        UUID cinemaId = UUID.randomUUID();
        given(cinemaRepository.findById(cinemaId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> cinemaService.deleteCinema(cinemaId, adminUser))
                .isInstanceOf(io.github.dushyna.ticketflow.exception.handling.exceptions.common.RestApiException.class)
                .hasFieldOrPropertyWithValue("httpStatus", org.springframework.http.HttpStatus.NOT_FOUND);

        verify(cinemaRepository, never()).delete(any());
    }

    @Test
    @DisplayName("deleteCinema: Failure - Forbidden to delete another organization's cinema")
    void deleteCinema_Forbidden_ThrowsException() {
        // Given
        UUID cinemaId = UUID.randomUUID();
        Organization otherOrg = new Organization();
        ReflectionTestUtils.setField(otherOrg, "id", UUID.randomUUID());

        Cinema cinema = new Cinema();
        cinema.setOrganization(otherOrg);

        given(cinemaRepository.findById(cinemaId)).willReturn(Optional.of(cinema));

        // When & Then
        assertThatThrownBy(() -> cinemaService.deleteCinema(cinemaId, adminUser))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Forbidden");

        verify(cinemaRepository, never()).delete(any());
    }

    @Test
    @DisplayName("deleteCinema: Edge Case - Super Admin can delete any cinema")
    void deleteCinema_SuperAdmin_Success() {
        // Given
        UUID cinemaId = UUID.randomUUID();
        AppUser superAdmin = new AppUser();
        superAdmin.setRole(Role.ROLE_SUPER_ADMIN);

        Cinema cinema = new Cinema();
        cinema.setOrganization(new Organization());

        given(cinemaRepository.findById(cinemaId)).willReturn(Optional.of(cinema));

        // When
        cinemaService.deleteCinema(cinemaId, superAdmin);

        // Then
        verify(cinemaRepository).delete(cinema);
    }

}
