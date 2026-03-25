package io.github.dushyna.ticketflow.user.service.impl;

import io.github.dushyna.ticketflow.user.dto.request.UpdateUserDetailsDto;
import io.github.dushyna.ticketflow.user.dto.response.UserResponseDto;
import io.github.dushyna.ticketflow.user.entity.AppUser;
import io.github.dushyna.ticketflow.user.exception.UserNotFoundException;
import io.github.dushyna.ticketflow.user.repository.UserRepository;
import io.github.dushyna.ticketflow.user.service.interfaces.UserService;
import io.github.dushyna.ticketflow.user.util.AppUserMapper;
import io.github.dushyna.ticketflow.user.util.UserUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for various operations with Employees
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository repository;
    private final AppUserMapper mappingService;

    @Override
    public AppUser saveOrUpdate(final AppUser user) {
        return repository.save(user);
    }

    @Override
    public Optional<AppUser> getByEmail(String email) {
        return repository.findByEmailIgnoreCase(email);
    }

    @Override
    public AppUser getByEmailOrThrow(String email) {
        return getByEmail(email)
                .orElseThrow(UserNotFoundException::new);
    }

    @Override
    public AppUser getByIdOrThrow(String id) {
        return repository
                .findById(UUID.fromString(id))
                .orElseThrow(UserNotFoundException::new);
    }

    @Override
    public List<UserResponseDto> getAll() {
        return repository.findAll().stream()
                .map(mappingService::mapEntityToResponseDto)
                .toList();
    }

    @Override
    @Transactional
    public UserResponseDto getUserDetails() {
        AppUser user = getCurrentUserOrThrow();
        return mappingService.mapEntityToResponseDto(user);
    }

    @Override
    @Transactional
    public UserResponseDto updateUserDetails(UpdateUserDetailsDto dto) {
        AppUser user = getCurrentUserOrThrow();

        if (dto.firstName() != null) {
            user.setFirstName(UserUtils.normalizeUserName(dto.firstName()));
        }

        if (dto.lastName() != null) {
            user.setLastName(UserUtils.normalizeUserName(dto.lastName()));
        }

        if (dto.birthDate() != null) {
            user.setBirthDate(dto.birthDate());
        }

        if (dto.phone() != null) {
            user.setPhone(dto.phone());
        }

        AppUser saved = repository.save(user);
        return mappingService.mapEntityToResponseDto(saved);
    }

    private AppUser getCurrentUserOrThrow() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new UserNotFoundException();
        }

        String email = auth.getName();
        return repository.findByEmailIgnoreCase(email)
                .orElseThrow(UserNotFoundException::new);
    }
}
