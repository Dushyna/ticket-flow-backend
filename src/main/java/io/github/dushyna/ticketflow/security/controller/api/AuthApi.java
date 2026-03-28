package io.github.dushyna.ticketflow.security.controller.api;

import io.github.dushyna.ticketflow.exception.handling.response.ErrorResponseDto;
import io.github.dushyna.ticketflow.security.dto.LoginRequest;
import io.github.dushyna.ticketflow.security.dto.request.ForgotPasswordRequestDto;
import io.github.dushyna.ticketflow.security.dto.request.ResetPasswordRequestDto;
import io.github.dushyna.ticketflow.user.dto.response.UserResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Authorization controller", description = "Secure HttpOnly Cookie based authorization")
@RequestMapping("/api/v1/auth")
public interface AuthApi {

    @Operation(summary = "Login", description = "Authenticates user and sets HttpOnly cookies")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully authenticated. Cookies are set.",
                    content = @Content(schema = @Schema(implementation = UserResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Invalid credentials",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    @PostMapping("/login")
    UserResponseDto login(@RequestBody @Valid LoginRequest loginRequest, HttpServletResponse response);

    @Operation(summary = "Refresh token", description = "Updates access token cookie using refresh token cookie")
    @PostMapping("/refresh-token")
    void refreshAccessToken(HttpServletRequest request, HttpServletResponse response);

    @Operation(summary = "Logout", description = "Clears authentication cookies")
    @PostMapping("/logout")
    void logout(HttpServletResponse response);

    @Operation(summary = "Get current user", description = "Returns user data if session cookie is valid")
    @SecurityRequirement(name = "cookieAuth")
    @GetMapping("/me")
    UserResponseDto getCurrentUser(@Parameter(hidden = true) @AuthenticationPrincipal Authentication authentication);

    @Operation(summary = "Forgot password")
    @PostMapping("/forgot-password")
    void forgotPassword(@RequestBody @Valid ForgotPasswordRequestDto request);

    @Operation(summary = "Validate reset token")
    @GetMapping("/reset-password/validate")
    void validateResetToken(@RequestParam String token);

    @Operation(summary = "Reset password")
    @PostMapping("/reset-password")
    void resetPassword(@RequestBody @Valid ResetPasswordRequestDto request);


}
