package io.github.dushyna.ticketflow.user.service.interfaces;

import io.github.dushyna.ticketflow.user.dto.request.UpdateUserDetailsDto;
import io.github.dushyna.ticketflow.user.dto.response.UserResponseDto;
import io.github.dushyna.ticketflow.user.entity.AppUser;

import java.util.List;
import java.util.Optional;

/**
 * Service for various operations with Employees
 */
public interface UserService {

    AppUser saveOrUpdate(AppUser user);

    Optional<AppUser> getByEmail(String email);

    AppUser getByEmailOrThrow(String email);

    AppUser getByIdOrThrow(String id);

    List<UserResponseDto> getAll();

    UserResponseDto getUserDetails();

    UserResponseDto updateUserDetails(UpdateUserDetailsDto dto);
}
