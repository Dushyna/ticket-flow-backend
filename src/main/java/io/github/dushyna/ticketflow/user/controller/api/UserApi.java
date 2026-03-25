package io.github.dushyna.ticketflow.user.controller.api;

import io.github.dushyna.ticketflow.user.dto.request.UpdateUserDetailsDto;
import io.github.dushyna.ticketflow.user.dto.response.UserResponseDto;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * REST mappings for user operations.
 * Implementation classes should implement this interface.
 */
@RequestMapping("/api/v1/users")
public interface UserApi extends UserApiSwaggerDoc {

    @Override
    @GetMapping("/all")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    List<UserResponseDto> getAll();



    @GetMapping("/me-details")
    UserResponseDto getUserDetails();

    @PatchMapping("/update-user")
    UserResponseDto updateUserDetails(@RequestBody UpdateUserDetailsDto request);
}
