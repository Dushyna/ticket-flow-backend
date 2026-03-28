package io.github.dushyna.ticketflow.user.service.interfaces;

import io.github.dushyna.ticketflow.user.dto.request.UpdateUserDetailsDto;
import io.github.dushyna.ticketflow.user.dto.response.UserResponseDto;
import io.github.dushyna.ticketflow.user.entity.AppUser;

import java.util.List;
import java.util.Optional;

/**
 * Service for various operations with Users/Employees.
 * Refactored to accept currentUser as an argument for better testability and decoupling.
 */
public interface UserService {

    AppUser saveOrUpdate(AppUser user);

    Optional<AppUser> getByEmail(String email);

    AppUser getByEmailOrThrow(String email);


    List<UserResponseDto> getAll();

    /**
     * Maps the provided user entity to a response DTO.
     * @param currentUser the user fetched in the controller/security layer
     */
    UserResponseDto getUserDetails(AppUser currentUser);

    /**
     * Updates details for the specifically provided user.
     * @param dto data to update
     * @param currentUser the target user entity
     */
    UserResponseDto updateUserDetails(UpdateUserDetailsDto dto, AppUser currentUser);
}
