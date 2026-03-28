package io.github.dushyna.ticketflow.user.controller;

import io.github.dushyna.ticketflow.user.controller.api.UserApi;
import io.github.dushyna.ticketflow.user.dto.request.UpdateUserDetailsDto;
import io.github.dushyna.ticketflow.user.dto.response.UserResponseDto;
import io.github.dushyna.ticketflow.user.entity.AppUser;
import io.github.dushyna.ticketflow.user.service.interfaces.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
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
    public List<UserResponseDto> getAll() {
        return service.getAll();
    }

    @Override
    public UserResponseDto getUserDetails(@AuthenticationPrincipal Jwt jwt) {
        AppUser currentUser = service.getByEmailOrThrow(jwt.getSubject());
        return service.getUserDetails(currentUser);
    }

    @Override
    public UserResponseDto updateUserDetails(UpdateUserDetailsDto request, @AuthenticationPrincipal Jwt jwt) {
        AppUser currentUser = service.getByEmailOrThrow(jwt.getSubject());
        return service.updateUserDetails(request, currentUser);
    }
}
