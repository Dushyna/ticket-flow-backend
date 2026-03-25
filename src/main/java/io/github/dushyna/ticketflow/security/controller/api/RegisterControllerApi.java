package io.github.dushyna.ticketflow.security.controller.api;

import io.github.dushyna.ticketflow.exception.handling.response.ErrorResponseDto;
import io.github.dushyna.ticketflow.exception.handling.response.ValidationErrorDto;
import io.github.dushyna.ticketflow.user.dto.request.TenantRegistrationDto;
import io.github.dushyna.ticketflow.user.dto.request.UserCreateDto;
import io.github.dushyna.ticketflow.user.dto.response.UserCreateResponseDto;
import io.github.dushyna.ticketflow.user.dto.response.UserResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Tag(name = "User Registration", description = "Endpoints for user registration (Customers and Business Owners)")
@RequestMapping("/api/v1/auth")
public interface RegisterControllerApi {

    @Operation(summary = "Register a Customer", description = "Creates a standard user account. Organization will be null.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Customer registered successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid payload",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = ValidationErrorDto.class))))
    })
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/register/customer")
    UserCreateResponseDto registerCustomer(@RequestBody @Valid UserCreateDto registerUser);

    @Operation(summary = "Register a Business Owner (Tenant)", description = "Simultaneously creates a new Organization and an Admin user linked to it.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Organization and Admin created successfully"),
            @ApiResponse(responseCode = "409", description = "Slug or Email already exists",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/register/tenant")
    UserCreateResponseDto registerTenant(@RequestBody @Valid TenantRegistrationDto registerTenant);

    @Operation(summary = "Confirm registration", description = "Confirms account using the code from email.")
    @GetMapping("/confirm/{code}")
    UserResponseDto confirmRegistration(@PathVariable String code);
}
