package io.github.dushyna.ticketflow.cinema.service.impl;

import io.github.dushyna.ticketflow.cinema.dto.request.TicketTypeRequestDto;
import io.github.dushyna.ticketflow.cinema.dto.response.TicketTypeResponseDto;
import io.github.dushyna.ticketflow.cinema.entity.TicketType;
import io.github.dushyna.ticketflow.cinema.repository.TicketTypeRepository;
import io.github.dushyna.ticketflow.cinema.utils.TicketTypeMapper;
import io.github.dushyna.ticketflow.organization.entity.Organization;
import io.github.dushyna.ticketflow.user.entity.AppUser;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit tests for TicketTypeServiceImpl")
class TicketTypeServiceImplTest {

    @Mock private TicketTypeRepository ticketTypeRepository;
    @Mock private TicketTypeMapper ticketTypeMapper;

    @InjectMocks
    private TicketTypeServiceImpl ticketTypeService;

    private AppUser currentUser;
    private Organization organization;
    private UUID orgId;

    @BeforeEach
    void setUp() {
        orgId = UUID.randomUUID();
        organization = new Organization();
        ReflectionTestUtils.setField(organization, "id", orgId);

        currentUser = new AppUser();
        currentUser.setOrganization(organization);
    }

    // --- createTicketType ---

    @Test
    @DisplayName("createTicketType: Success - should reset defaults if new type is default")
    void createTicketType_Default_Success() {
        // Given
        TicketTypeRequestDto dto = new TicketTypeRequestDto("Student", new BigDecimal("0.8"), true);
        TicketType ticketType = new TicketType();

        given(ticketTypeMapper.mapRequestDtoToEntity(dto)).willReturn(ticketType);
        given(ticketTypeRepository.save(any())).willReturn(ticketType);
        given(ticketTypeMapper.mapEntityToResponseDto(any())).willReturn(mock(TicketTypeResponseDto.class));

        // When
        ticketTypeService.createTicketType(dto, currentUser);

        // Then
        verify(ticketTypeRepository).resetDefaultsByOrganizationId(orgId);
        verify(ticketTypeRepository).save(ticketType);
        assertThat(ticketType.getOrganization()).isEqualTo(organization);
    }

    @Test
    @DisplayName("createTicketType: Success - should NOT reset defaults if new type is NOT default")
    void createTicketType_NonDefault_Success() {
        // Given
        TicketTypeRequestDto dto = new TicketTypeRequestDto("Adult", BigDecimal.ONE, false);
        TicketType ticketType = new TicketType();

        given(ticketTypeMapper.mapRequestDtoToEntity(dto)).willReturn(ticketType);
        given(ticketTypeRepository.save(any())).willReturn(ticketType);
        given(ticketTypeMapper.mapEntityToResponseDto(any())).willReturn(mock(TicketTypeResponseDto.class));

        // When
        ticketTypeService.createTicketType(dto, currentUser);

        // Then
        verify(ticketTypeRepository, never()).resetDefaultsByOrganizationId(any());
        verify(ticketTypeRepository).save(ticketType);
    }

    @Test
    @DisplayName("createTicketType: Failure - User without organization")
    void createTicketType_NoOrg_ThrowsException() {
        currentUser.setOrganization(null);
        assertThatThrownBy(() -> ticketTypeService.createTicketType(mock(TicketTypeRequestDto.class), currentUser))
                .isInstanceOf(AccessDeniedException.class);
    }

    // --- updateTicketType ---

    @Test
    @DisplayName("updateTicketType: Success - should reset defaults if changing false to true")
    void updateTicketType_ChangeToDefault_Success() {
        // Given
        UUID id = UUID.randomUUID();
        TicketTypeRequestDto dto = new TicketTypeRequestDto("Promo", BigDecimal.TEN, true);
        TicketType existingType = new TicketType();
        existingType.setOrganization(organization);
        existingType.setDefault(false); // Was not default

        given(ticketTypeRepository.findById(id)).willReturn(Optional.of(existingType));
        given(ticketTypeRepository.save(any())).willReturn(existingType);
        given(ticketTypeMapper.mapEntityToResponseDto(any())).willReturn(mock(TicketTypeResponseDto.class));

        // When
        ticketTypeService.updateTicketType(id, dto, currentUser);

        // Then
        verify(ticketTypeRepository).resetDefaultsByOrganizationId(orgId);
        verify(ticketTypeMapper).updateEntityFromDto(dto, existingType);
    }

    @Test
    @DisplayName("updateTicketType: Success - should NOT reset defaults if already default")
    void updateTicketType_KeepDefault_Success() {
        // Given
        UUID id = UUID.randomUUID();
        TicketTypeRequestDto dto = new TicketTypeRequestDto("Promo", BigDecimal.TEN, true);
        TicketType existingType = new TicketType();
        existingType.setOrganization(organization);
        existingType.setDefault(true); // Already default

        given(ticketTypeRepository.findById(id)).willReturn(Optional.of(existingType));
        given(ticketTypeRepository.save(any())).willReturn(existingType);
        given(ticketTypeMapper.mapEntityToResponseDto(any())).willReturn(mock(TicketTypeResponseDto.class));

        // When
        ticketTypeService.updateTicketType(id, dto, currentUser);

        // Then
        verify(ticketTypeRepository, never()).resetDefaultsByOrganizationId(any());
    }

    @Test
    @DisplayName("updateTicketType: Failure - Forbidden for other organization")
    void updateTicketType_Forbidden() {
        UUID id = UUID.randomUUID();
        TicketType otherType = new TicketType();
        Organization otherOrg = new Organization();
        ReflectionTestUtils.setField(otherOrg, "id", UUID.randomUUID());
        otherType.setOrganization(otherOrg);

        given(ticketTypeRepository.findById(id)).willReturn(Optional.of(otherType));

        assertThatThrownBy(() -> ticketTypeService.updateTicketType(id, mock(TicketTypeRequestDto.class), currentUser))
                .isInstanceOf(AccessDeniedException.class);
    }

    // --- getTicketTypesByOrganization (Current User) ---

    @Test
    @DisplayName("getTicketTypesByOrganization: Success - should return list for current user org")
    void getTicketTypesByOrganization_Success() {
        // Given
        given(ticketTypeRepository.findAllByOrganizationId(orgId))
                .willReturn(List.of(new TicketType()));
        given(ticketTypeMapper.mapEntityToResponseDto(any()))
                .willReturn(mock(TicketTypeResponseDto.class));

        // When
        List<TicketTypeResponseDto> result = ticketTypeService.getTicketTypesByOrganization(currentUser);

        // Then
        assertThat(result).hasSize(1);
        verify(ticketTypeRepository).findAllByOrganizationId(orgId);
    }

    @Test
    @DisplayName("getTicketTypesByOrganization: Failure - User without organization")
    void getTicketTypesByOrganization_NoOrg_ThrowsException() {
        // Given
        currentUser.setOrganization(null);

        // When & Then
        assertThatThrownBy(() -> ticketTypeService.getTicketTypesByOrganization(currentUser))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("User must belong to an organization");
    }

    // --- getTicketTypesByOrganizationId (By ID) ---

    @Test
    @DisplayName("getTicketTypesByOrganizationId: Success - should return list by provided UUID")
    void getTicketTypesByOrganizationId_Success() {
        // Given
        UUID targetOrgId = UUID.randomUUID();
        given(ticketTypeRepository.findAllByOrganizationId(targetOrgId))
                .willReturn(List.of(new TicketType(), new TicketType()));
        given(ticketTypeMapper.mapEntityToResponseDto(any()))
                .willReturn(mock(TicketTypeResponseDto.class));

        // When
        List<TicketTypeResponseDto> result = ticketTypeService.getTicketTypesByOrganizationId(targetOrgId);

        // Then
        assertThat(result).hasSize(2);
        verify(ticketTypeRepository).findAllByOrganizationId(targetOrgId);
    }

    @Test
    @DisplayName("getTicketTypesByOrganizationId: Edge Case - should return empty list if org has no types")
    void getTicketTypesByOrganizationId_Empty() {
        // Given
        UUID targetOrgId = UUID.randomUUID();
        given(ticketTypeRepository.findAllByOrganizationId(targetOrgId)).willReturn(List.of());

        // When
        List<TicketTypeResponseDto> result = ticketTypeService.getTicketTypesByOrganizationId(targetOrgId);

        // Then
        assertThat(result).isEmpty();
        verify(ticketTypeRepository).findAllByOrganizationId(targetOrgId);
    }


    @Test
    @DisplayName("deleteTicketType: Success")
    void deleteTicketType_Success() {
        UUID id = UUID.randomUUID();
        TicketType type = new TicketType();
        type.setOrganization(organization);

        given(ticketTypeRepository.findById(id)).willReturn(Optional.of(type));

        ticketTypeService.deleteTicketType(id, currentUser);

        verify(ticketTypeRepository).delete(type);
    }

    @Test
    @DisplayName("deleteTicketType: Failure - Forbidden to delete ticket type of another organization")
    void deleteTicketType_Forbidden_ThrowsException() {
        // Given
        UUID id = UUID.randomUUID();

        // Ticket type belongs to a DIFFERENT organization
        Organization otherOrg = new Organization();
        ReflectionTestUtils.setField(otherOrg, "id", UUID.randomUUID());

        TicketType ticketType = new TicketType();
        ticketType.setOrganization(otherOrg);

        given(ticketTypeRepository.findById(id)).willReturn(Optional.of(ticketType));

        // When & Then
        assertThatThrownBy(() -> ticketTypeService.deleteTicketType(id, currentUser))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("No access to this ticket type");

        verify(ticketTypeRepository, never()).delete(any());
    }

    @Test
    @DisplayName("deleteTicketType: Failure - should throw exception when ticket type not found")
    void deleteTicketType_NotFound_ThrowsException() {
        // Given
        UUID fakeId = UUID.randomUUID();
        given(ticketTypeRepository.findById(fakeId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> ticketTypeService.deleteTicketType(fakeId, currentUser))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Ticket type not found");

        verify(ticketTypeRepository, never()).delete(any());
    }

}
