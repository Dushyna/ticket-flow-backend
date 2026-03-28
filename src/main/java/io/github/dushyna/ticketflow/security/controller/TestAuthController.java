package io.github.dushyna.ticketflow.security.controller;

import io.github.dushyna.ticketflow.security.dto.AuthUserDetails;
import io.github.dushyna.ticketflow.user.dto.response.UserResponseDto;
import io.github.dushyna.ticketflow.user.utils.AppUserMapper;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.PermitAll;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("dev")
@RequestMapping("/api/v1/test/auth")
@RequiredArgsConstructor
@Tag(name = "Test Auth controller")
public class TestAuthController {


    private final AppUserMapper appUserMapper;

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping("/role/admin")
    public UserResponseDto adminReq(@AuthenticationPrincipal
                                    @Parameter(hidden = true)
                                    AuthUserDetails principal) {
        return appUserMapper.mapEntityToResponseDto(principal.user());
    }

    @PreAuthorize("hasRole('ROLE_USER')")
    @GetMapping("/role/user")
    public UserResponseDto userReq(@AuthenticationPrincipal
                                   @Parameter(hidden = true)
                                   AuthUserDetails principal) {
        return appUserMapper.mapEntityToResponseDto(principal.user());
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/role/any")
    public UserResponseDto anyAuth(@AuthenticationPrincipal
                                   @Parameter(hidden = true)
                                   AuthUserDetails principal) {
        return appUserMapper.mapEntityToResponseDto(principal.user());
    }

    @PermitAll
    @GetMapping("/permit-all")
    public String permitAll() {
        return "Permit All";
    }

    @GetMapping("/not-annotated")
    public String notAnnotated() {
        return "Not Annotated";
    }
}
