package io.github.dushyna.ticketflow.cinema.service.impl;

import io.github.dushyna.ticketflow.cinema.dto.request.TicketTypeRequestDto;
import io.github.dushyna.ticketflow.cinema.dto.response.TicketTypeResponseDto;
import io.github.dushyna.ticketflow.cinema.entity.TicketType;
import io.github.dushyna.ticketflow.cinema.repository.TicketTypeRepository;
import io.github.dushyna.ticketflow.cinema.service.interfaces.TicketTypeService;
import io.github.dushyna.ticketflow.cinema.utils.TicketTypeMapper;
import io.github.dushyna.ticketflow.user.entity.AppUser;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TicketTypeServiceImpl implements TicketTypeService {

    private final TicketTypeRepository ticketTypeRepository;
    private final TicketTypeMapper ticketTypeMapper;

    @Override
    @Transactional
    public TicketTypeResponseDto createTicketType(TicketTypeRequestDto dto, AppUser currentUser) {
        if (currentUser.getOrganization() == null) {
            throw new AccessDeniedException("User must belong to an organization");
        }

        if (dto.isDefault()) {
            resetDefaultTicketType(currentUser.getOrganization().getId());
        }

        TicketType ticketType = ticketTypeMapper.mapRequestDtoToEntity(dto);
        ticketType.setOrganization(currentUser.getOrganization());

        return ticketTypeMapper.mapEntityToResponseDto(ticketTypeRepository.save(ticketType));
    }

    @Override
    @Transactional
    public TicketTypeResponseDto updateTicketType(UUID id, TicketTypeRequestDto dto, AppUser currentUser) {
        TicketType ticketType = ticketTypeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Ticket type not found"));

        validateAccess(ticketType, currentUser);

        if (dto.isDefault() && !ticketType.isDefault()) {
            resetDefaultTicketType(currentUser.getOrganization().getId());
        }

        ticketTypeMapper.updateEntityFromDto(dto, ticketType);
        return ticketTypeMapper.mapEntityToResponseDto(ticketTypeRepository.save(ticketType));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketTypeResponseDto> getTicketTypesByOrganization(AppUser currentUser) {
        if (currentUser.getOrganization() == null) {
            throw new AccessDeniedException("User must belong to an organization");
        }

        return ticketTypeRepository.findAllByOrganizationId(currentUser.getOrganization().getId())
                .stream()
                .map(ticketTypeMapper::mapEntityToResponseDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketTypeResponseDto> getTicketTypesByOrganizationId(UUID organizationId) {
        return ticketTypeRepository.findAllByOrganizationId(organizationId)
                .stream()
                .map(ticketTypeMapper::mapEntityToResponseDto)
                .toList();
    }

    @Override
    @Transactional
    public void deleteTicketType(UUID id, AppUser currentUser) {
        TicketType ticketType = ticketTypeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Ticket type not found"));

        validateAccess(ticketType, currentUser);

        ticketTypeRepository.delete(ticketType);
    }

    private void resetDefaultTicketType(UUID organizationId) {
        ticketTypeRepository.resetDefaultsByOrganizationId(organizationId);
    }

    private void validateAccess(TicketType ticketType, AppUser user) {
        if (user == null || user.getOrganization() == null ||
                !ticketType.getOrganization().getId().equals(user.getOrganization().getId())) {
            throw new AccessDeniedException("No access to this ticket type");
        }
    }
}
