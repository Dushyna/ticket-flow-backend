package io.github.dushyna.ticketflow.user.service.impl;

import io.github.dushyna.ticketflow.exception.handling.exceptions.common.RestApiException;
import io.github.dushyna.ticketflow.user.dto.request.UpdateUserDetailsDto;
import io.github.dushyna.ticketflow.user.dto.request.UserRoleUpdateRequestDto;
import io.github.dushyna.ticketflow.user.dto.response.UserResponseDto;
import io.github.dushyna.ticketflow.user.entity.AppUser;
import io.github.dushyna.ticketflow.user.entity.Role;
import io.github.dushyna.ticketflow.user.exception.UserNotFoundException;
import io.github.dushyna.ticketflow.user.repository.UserRepository;
import io.github.dushyna.ticketflow.user.service.interfaces.UserService;
import io.github.dushyna.ticketflow.user.utils.AppUserMapper;
import io.github.dushyna.ticketflow.user.utils.UserUtils;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

/**
 * Service for various operations with Employees
 */
@Service
@RequiredArgsConstructor
@Slf4j
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

    @Override
    @Transactional(readOnly = true)
    public Optional<AppUser> findByEmailWithOrganization(String email) {
        return repository.findByEmailWithOrganization(email);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponseDto> getAllManagedUsers(AppUser currentUser) {
        List<AppUser> users;

        if (currentUser.getRole() == Role.ROLE_SUPER_ADMIN) {
            // Super Admin gets a global system layout vision override
            users = repository.findAll();
        } else {
            // Tenant Admin views his current team AND unassigned customers to invite them
            if (currentUser.getOrganization() == null) {
                throw new RestApiException(
                        HttpStatus.FORBIDDEN,
                        "Access Denied: Current administrator profile missing an assigned organization identity."
                );
            }
            users = repository.findAllManagedAndAvailableUsers(currentUser.getOrganization().getId());
        }

        return users.stream()
                .map(mappingService::mapEntityToResponseDto)
                .toList();
    }

    /**
     * Updates a user role (granting ROLE_CASHIER) with absolute tenant-isolation protection.
     */
// Inside UserServiceImpl.java -> updateUserRole method

    @Override
    @Transactional
    public UserResponseDto updateUserRole(UserRoleUpdateRequestDto request, AppUser currentUser) {
        AppUser targetUser = repository.findById(request.userId())
                .orElseThrow(() -> new RestApiException(HttpStatus.NOT_FOUND, "Target user not found"));

        // Strict multi-tenancy validation barrier
        if (currentUser.getRole() != Role.ROLE_SUPER_ADMIN) {
            // 1. Check if the logged-in administrator actually belongs to a cinema organization
            if (currentUser.getOrganization() == null) {
                throw new RestApiException(HttpStatus.FORBIDDEN, "Access Denied: Admin has no assigned organization.");
            }

            // 2.  Allow modification if target user belongs to the SAME organization OR has NO organization yet (is unassigned)
            boolean isSameOrganization = targetUser.getOrganization() != null &&
                    targetUser.getOrganization().getId().equals(currentUser.getOrganization().getId());
            boolean isUnassignedUser = targetUser.getOrganization() == null;

            if (!isSameOrganization && !isUnassignedUser) {
                throw new RestApiException(
                        HttpStatus.FORBIDDEN,
                        "Access Denied: You cannot modify employee roles belonging to other cinema organizations."
                );
            }
        }

        // Apply corporate auto-binding mechanism for cashier staff assignments
        if (request.role() == Role.ROLE_CASHIER) {
            if (currentUser.getRole() == Role.ROLE_TENANT_ADMIN) {
                targetUser.setOrganization(currentUser.getOrganization());
            }
        } else if (request.role() == Role.ROLE_USER) {
            // Detach organization schema if demoted back to a standard customer node
            targetUser.setOrganization(null);
        }

        targetUser.setRole(request.role());
        AppUser savedUser = repository.save(targetUser);

        log.info("SECURITY IDENTITY: User {} role updated to {} by administrator {}",
                targetUser.getEmail(), request.role(), currentUser.getEmail());

        return mappingService.mapEntityToResponseDto(savedUser);
    }
}
