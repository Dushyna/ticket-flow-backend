package io.github.dushyna.ticketflow.user.controller;

import io.github.dushyna.ticketflow.user.controller.api.UserApi;
import io.github.dushyna.ticketflow.security.dto.AuthUserDetails; // Added
import io.github.dushyna.ticketflow.user.dto.request.UpdateUserDetailsDto;
import io.github.dushyna.ticketflow.user.dto.request.UserRoleUpdateRequestDto;
import io.github.dushyna.ticketflow.user.dto.response.UserResponseDto;
import io.github.dushyna.ticketflow.user.service.interfaces.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

/**
 * REST Controller that receives http-requests for various operations with Users
 */
@RestController
@RequiredArgsConstructor
public class UserController implements UserApi {

    private final UserService service;

    @Override
    public List<UserResponseDto> getAll(@AuthenticationPrincipal AuthUserDetails userDetails) {
        return service.getAllManagedUsers(userDetails.user());
    }

    @Override
    public UserResponseDto updateUserRole(UserRoleUpdateRequestDto request, @AuthenticationPrincipal AuthUserDetails userDetails) {
        return service.updateUserRole(request, userDetails.user());
    }

    @Override
    public UserResponseDto getUserDetails(@AuthenticationPrincipal AuthUserDetails userDetails) {
        return service.getUserDetails(userDetails.user());
    }

    @Override
    public UserResponseDto updateUserDetails(UpdateUserDetailsDto request, @AuthenticationPrincipal AuthUserDetails userDetails) {
        return service.updateUserDetails(request, userDetails.user());
    }
}
