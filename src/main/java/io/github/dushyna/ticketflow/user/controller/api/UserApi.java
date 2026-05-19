package io.github.dushyna.ticketflow.user.controller.api;

import io.github.dushyna.ticketflow.exception.handling.response.ErrorResponseDto;
import io.github.dushyna.ticketflow.security.dto.AuthUserDetails; // Added explicit import
import io.github.dushyna.ticketflow.user.dto.request.UpdateUserDetailsDto;
import io.github.dushyna.ticketflow.user.dto.request.UserRoleUpdateRequestDto;
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
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    UserResponseDto getUserDetails(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthUserDetails userDetails
    );

    @Operation(summary = "Update current user profile")
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("/update-user")
    UserResponseDto updateUserDetails(
            @RequestBody UpdateUserDetailsDto request,
            @Parameter(hidden = true) @AuthenticationPrincipal AuthUserDetails userDetails
    );

    @Operation(
            summary = "Get all users",
            description = "Returns a JSON array of all users managed by the current administrator context hierarchy"
    )
    @SecurityRequirement(name = "cookieAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Users retrieved successfully",
                    content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = UserResponseDto.class)))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    @GetMapping("/all")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_TENANT_ADMIN')")
    List<UserResponseDto> getAll(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthUserDetails userDetails
    );

    @Operation(summary = "Update user role inside organization scope")
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("/role")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_TENANT_ADMIN')")
    UserResponseDto updateUserRole(
            @Valid @RequestBody UserRoleUpdateRequestDto request,
            @Parameter(hidden = true) @AuthenticationPrincipal AuthUserDetails userDetails
    );
}
