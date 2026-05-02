package io.github.dushyna.ticketflow.user.controller.api;

import io.github.dushyna.ticketflow.exception.handling.response.ErrorResponseDto;
import io.github.dushyna.ticketflow.user.dto.request.UpdateUserDetailsDto;
import io.github.dushyna.ticketflow.user.dto.response.UserResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(
        name = "User Management",
        description = "Operations to manage application users"
)
@RequestMapping("/api/v1/users")
public interface UserApi {

    @Operation(summary = "Get current user details")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/me-details")
    UserResponseDto getUserDetails(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt);

    @Operation(summary = "Update current user profile")
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("/update-user")
    UserResponseDto updateUserDetails(
            @RequestBody UpdateUserDetailsDto request,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt
    );

    @Operation(
            summary = "Get all users",
            description = "Returns a JSON array of all users in the system"
    )
    @SecurityRequirement(name = "cookieAuth")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Users retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(
                                    schema = @Schema(implementation = UserResponseDto.class)
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDto.class))
            )
    })
    @GetMapping("/all")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    List<UserResponseDto> getAll();
}
