package io.github.dushyna.ticketflow.security.controller.api;

import io.github.dushyna.ticketflow.exception.handling.response.ErrorResponseDto;
import io.github.dushyna.ticketflow.security.dto.LoginRequest;
import io.github.dushyna.ticketflow.security.dto.request.ForgotPasswordRequestDto;
import io.github.dushyna.ticketflow.security.dto.request.ResetPasswordRequestDto;
import io.github.dushyna.ticketflow.security.dto.response.TokenResponseDto;
import io.github.dushyna.ticketflow.user.dto.response.UserResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Authorization API description for Swagger
 */
@Tag(name = "Authorization controller", description = "Controller for User authorization")
@RequestMapping("/api/v1/auth")
public interface AuthApi {

    @Operation(summary = "Login", description = "User login process")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Authorization successfully completed",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = TokenResponseDto.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "accessToken": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXNfZGV2QHVwdGVhbXMuZGUiLCJleHAiOjE3NTA5Mzc5NTh9.0ADS106wR9fuLuAXCgzFyxDP4JaznPH7zCZgUy_11GQ",
                                      "refreshToken": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXNfZGV2QHVwdGVhbXMuZGUiLCJleHAiOjE3NTEwMjM3NDd9.joEW3Hv8E63cWYf2w-bHr7pQEgUycYpWEF-Hq0r8Yrs"
                                    }
                                    """)
                    )
            ),
            @ApiResponse(responseCode = "401", description = "Incorrect credentials, unconfirmed registration user deactivated",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDto.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "timestamp": "2025-06-26T13:41:32.3327347",
                                      "status": 401,
                                      "error": "Unauthorized",
                                      "message": "User not found: <user email>",
                                      "path": "/api/v1/auth/login"
                                    }
                                    """)
                    )
            )
    })
    @PostMapping("/login")
    TokenResponseDto login(
            @RequestBody
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Instance of User with name and password"
            )
            @Valid
            LoginRequest loginRequest,
            HttpServletResponse response
    );

    @Operation(summary = "Get new access token",
            description = "Obtain new access token using a refresh token stored in httpOnly cookie")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "New access token granted",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = TokenResponseDto.class)))
            ,
            @ApiResponse(responseCode = "401", description = "Invalid or expired refresh token",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDto.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "timestamp": 1627660173000,
                                      "status": 401,
                                      "error": "Unauthorized",
                                      "message": "Refresh token is invalid or expired",
                                      "path": "/api/v1/auth/refresh-token"
                                    }
                                    """))
            )
    })
    @PostMapping("/refresh-token")
    TokenResponseDto refreshAccessToken(
            HttpServletRequest request,
            HttpServletResponse response
    );

    @Operation(summary = "Logout", description = "User logout")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Logout successful",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = TokenResponseDto.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "accessToken": null,
                                      "refreshToken": null
                                    }
                                    """)))
    })
    @PostMapping("/logout")
    TokenResponseDto logout(HttpServletResponse response);

    @Operation(summary = "Get current authenticated user",
            description = "Returns current user based on access token stored in httpOnly cookie")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Authenticated user"),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @GetMapping("/me")
    UserResponseDto getCurrentUser(
            @AuthenticationPrincipal Authentication authentication
    );

    @Operation(summary = "Forgot password",
            description ="Sends an email to change your password.")
    @PostMapping("/forgot-password")
    void forgotPassword(
            @RequestBody @Valid ForgotPasswordRequestDto request
    );

    @Operation(summary = "Validate reset password token")
    @GetMapping("/reset-password/validate")
    void validateResetToken(
            @RequestParam String token
    );

    @Operation(summary = "Change password")
    @PostMapping("/reset-password")
    void resetPassword(
            @RequestBody @Valid ResetPasswordRequestDto request
    );
}
