package io.github.dushyna.ticketflow.security.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.dushyna.ticketflow.user.dto.request.OrganizationCreateDto;
import io.github.dushyna.ticketflow.user.dto.request.TenantRegistrationDto;
import io.github.dushyna.ticketflow.user.dto.request.UserCreateDto;
import io.github.dushyna.ticketflow.user.dto.response.UserCreateResponseDto;
import io.github.dushyna.ticketflow.user.dto.response.UserResponseDto;
import io.github.dushyna.ticketflow.user.entity.ConfirmationStatus;
import io.github.dushyna.ticketflow.user.service.impl.UserRegisterServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Locale;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.web.locale=en")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RegisterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserRegisterServiceImpl registerService;

    @Test
    @DisplayName("POST /api/v1/auth/register/customer - Success: Returns 201 Created")
    void registerCustomer_Success() throws Exception {
        // Given
        UserCreateDto request = new UserCreateDto("customer@test.com", "Password123");
        UserCreateResponseDto response = new UserCreateResponseDto("id-1", "customer@test.com", "ROLE_USER", true);

        when(registerService.registerCustomer(any(UserCreateDto.class))).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/v1/auth/register/customer")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated()) // Verifying 201 status
                .andExpect(jsonPath("$.email").value("customer@test.com"));
    }


    @Test
    @DisplayName("POST /api/v1/auth/register/customer - Failure: Validation Error (Invalid Email)")
    void registerCustomer_InvalidEmail_ReturnsBadRequest() throws Exception {
        // Given: Providing malformed email to trigger @Valid
        UserCreateDto invalidRequest = new UserCreateDto("not-an-email", "pass");

        // When & Then
        mockMvc.perform(post("/api/v1/auth/register/customer")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/auth/register/customer - Failure: Weak Password")
    void registerCustomer_WeakPassword_ReturnsBadRequest() throws Exception {
        Locale.setDefault(Locale.ENGLISH);
        // Given: Password lacks uppercase letter and number, shorter than 8 chars
        UserCreateDto invalidRequest = new UserCreateDto("valid@email.com", "weak");

        // When & Then
        mockMvc.perform(post("/api/v1/auth/register/customer")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                // Verify that our GlobalExceptionHandler returns the specific validation message
                .andExpect(jsonPath("$.errors[0].messages[0]")
                        .value("Password must be at least 8 characters long and contain at least one uppercase letter, one lowercase letter, and one number."));
    }

    @Test
    @DisplayName("POST /api/v1/auth/register/customer - Failure: Invalid Email Regexp")
    void registerCustomer_MalformedEmail_ReturnsBadRequest() throws Exception {
        // Given: Email that fails your complex regex (e.g., missing top-level domain)
        Locale.setDefault(Locale.ENGLISH);
        UserCreateDto invalidRequest = new UserCreateDto("john.doe@sub", "StrongPass123");

        // When & Then
        mockMvc.perform(post("/api/v1/auth/register/customer")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].messages[0]").value("Invalid email format"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/register/tenant - Success: Returns 201 Created")
    void registerTenant_Success() throws Exception {
        // Given
        TenantRegistrationDto request = createValidTenantRegistrationDto();

        UserCreateResponseDto response = new UserCreateResponseDto(
                "org-id-123",
                "admin@cinema.com",
                "Organization and Admin registered successfully",
                true
        );

        when(registerService.registerTenant(any(TenantRegistrationDto.class))).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/v1/auth/register/tenant")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("admin@cinema.com"))
                .andExpect(jsonPath("$.role").exists());
    }


    @Test
    @DisplayName("POST /api/v1/auth/register/tenant - Failure: Invalid Slug format")
    void registerTenant_InvalidSlug_ReturnsBadRequest() throws Exception {
        // Given: Slug contains uppercase and spaces, violating @Pattern
        UserCreateDto adminDto = new UserCreateDto("admin@test.com", "SecurePass123");
        OrganizationCreateDto invalidOrgDto = new OrganizationCreateDto("Name", "INVALID SLUG", "email@test.com");
        TenantRegistrationDto request = new TenantRegistrationDto(adminDto, invalidOrgDto);

        // When & Then
        mockMvc.perform(post("/api/v1/auth/register/tenant")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").exists());
    }


    @Test
    @DisplayName("GET /api/v1/auth/confirm/{code} - Success")
    void confirmRegistration_Success() throws Exception {
        // Given
        String code = "valid-confirmation-code";
        UserResponseDto responseDto = new UserResponseDto(
                "id-123", "test@test.com", "John", "Doe", null, null,
                "ROLE_USER", ConfirmationStatus.CONFIRMED, null
        );

        when(registerService.confirmRegistration(code)).thenReturn(responseDto);

        // When & Then
        mockMvc.perform(get("/api/v1/auth/confirm/{code}", code))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.confirmationStatus").value("CONFIRMED"));
    }

    @Test
    @DisplayName("GET /api/v1/auth/confirm-redirect/{code} - Success: Redirects with param")
    void confirmEmailRedirect_Success() throws Exception {
        // Given
        String code = "secret-code";
        // This method is called in the controller before redirection
        when(registerService.confirmRegistration(code)).thenReturn(null);

        // When & Then
        mockMvc.perform(get("/api/v1/auth/confirm-redirect/{code}", code))
                .andDo(print())
                .andExpect(status().isFound()) // 302 Found
                .andExpect(header().exists("Location"))
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("confirmed=true")));
    }

    // Helper method to resolve the long method warning
    private TenantRegistrationDto createValidTenantRegistrationDto() {
        UserCreateDto adminDto = new UserCreateDto(
                "admin@cinema.com",
                "SecurePass123"
        );

        OrganizationCreateDto orgDto = new OrganizationCreateDto(
                "Dream Cinema",
                "dream-cinema",
                "contact@dreamcinema.com"
        );

        return new TenantRegistrationDto(adminDto, orgDto);
    }

}
