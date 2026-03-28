package io.github.dushyna.ticketflow.user.service.impl;

import io.github.dushyna.ticketflow.user.dto.request.UpdateUserDetailsDto;
import io.github.dushyna.ticketflow.user.dto.response.UserResponseDto;
import io.github.dushyna.ticketflow.user.entity.AppUser;
import io.github.dushyna.ticketflow.user.exception.UserNotFoundException;
import io.github.dushyna.ticketflow.user.repository.UserRepository;
import io.github.dushyna.ticketflow.user.service.interfaces.UserService;
import io.github.dushyna.ticketflow.user.utils.AppUserMapper;
import io.github.dushyna.ticketflow.user.utils.UserUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

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
    public List<UserResponseDto> getAll() {
        return repository.findAll().stream()
                .map(mappingService::mapEntityToResponseDto)
                .toList();
    }

    @Override
    @Transactional
    public UserResponseDto getUserDetails(AppUser currentUser) {
        return mappingService.mapEntityToResponseDto(currentUser);
    }

    @Override
    @Transactional
    public UserResponseDto updateUserDetails(UpdateUserDetailsDto dto, AppUser currentUser) {
        if (dto.firstName() != null) {
            currentUser.setFirstName(UserUtils.normalizeUserName(dto.firstName()));
        }

        if (dto.lastName() != null) {
            currentUser.setLastName(UserUtils.normalizeUserName(dto.lastName()));
        }

        if (dto.birthDate() != null) {
            currentUser.setBirthDate(dto.birthDate());
        }

        if (dto.phone() != null) {
            currentUser.setPhone(dto.phone());
        }

        AppUser saved = repository.save(currentUser);
        return mappingService.mapEntityToResponseDto(saved);
    }
}
