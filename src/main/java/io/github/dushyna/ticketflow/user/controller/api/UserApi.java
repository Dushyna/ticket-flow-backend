package io.github.dushyna.ticketflow.user.controller.api;

import io.github.dushyna.ticketflow.user.dto.request.UpdateUserDetailsDto;
import io.github.dushyna.ticketflow.user.dto.response.UserResponseDto;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RequestMapping("/api/v1/users")
public interface UserApi extends UserApiSwaggerDoc {

    @Override
    @GetMapping("/all")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    List<UserResponseDto> getAll();

    @GetMapping("/me-details")
    UserResponseDto getUserDetails(@AuthenticationPrincipal Jwt jwt);

    @PatchMapping("/update-user")
    UserResponseDto updateUserDetails(
            @RequestBody UpdateUserDetailsDto request,
            @AuthenticationPrincipal Jwt jwt
    );
}
