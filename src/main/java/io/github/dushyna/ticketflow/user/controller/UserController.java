package io.github.dushyna.ticketflow.user.controller;

import io.github.dushyna.ticketflow.user.controller.api.UserApi;
import io.github.dushyna.ticketflow.user.dto.request.UpdateUserDetailsDto;
import io.github.dushyna.ticketflow.user.dto.response.UserResponseDto;
import io.github.dushyna.ticketflow.user.service.interfaces.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

/**
 * REST Controller that receives http-requests for various operations with Employees
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
    public UserResponseDto getUserDetails() {
        return service.getUserDetails();
    }

    @Override
    public UserResponseDto updateUserDetails(UpdateUserDetailsDto request) {
        return service.updateUserDetails(request);
    }

}
