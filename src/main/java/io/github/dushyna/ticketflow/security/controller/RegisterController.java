package io.github.dushyna.ticketflow.security.controller;

import io.github.dushyna.ticketflow.security.controller.api.RegisterControllerApi;
import io.github.dushyna.ticketflow.user.dto.request.TenantRegistrationDto;
import io.github.dushyna.ticketflow.user.dto.request.UserCreateDto;
import io.github.dushyna.ticketflow.user.dto.response.UserCreateResponseDto;
import io.github.dushyna.ticketflow.user.dto.response.UserResponseDto;
import io.github.dushyna.ticketflow.user.service.impl.UserRegisterServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequiredArgsConstructor
public class RegisterController implements RegisterControllerApi {

    private final UserRegisterServiceImpl service;

    @Override
    public UserCreateResponseDto registerCustomer(UserCreateDto registerUser) {
        return service.registerCustomer(registerUser);
    }

    @Override
    public UserCreateResponseDto registerTenant(TenantRegistrationDto registerTenant) {
        return service.registerTenant(registerTenant);
    }

    @Override
    public UserResponseDto confirmRegistration(String code) {
        return service.confirmRegistration(code);
    }

    @GetMapping("/confirm-redirect/{code}")
    public ResponseEntity<Void> confirmEmailRedirect(@PathVariable String code) {
        service.confirmRegistration(code);
        URI redirectUri = URI.create("http://localhost:5173/#/login?confirmed=true");
        return ResponseEntity.status(HttpStatus.FOUND).location(redirectUri).build();
    }
}
