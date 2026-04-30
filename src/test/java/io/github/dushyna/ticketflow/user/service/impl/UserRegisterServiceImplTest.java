package io.github.dushyna.ticketflow.user.service.impl;

import io.github.dushyna.ticketflow.mail.EmailService;
import io.github.dushyna.ticketflow.mail.confirmation.code.ConfirmationCode;
import io.github.dushyna.ticketflow.mail.confirmation.code.interfaces.ConfirmationService;
import io.github.dushyna.ticketflow.organization.entity.Organization;
import io.github.dushyna.ticketflow.organization.repository.OrganizationRepository;
import io.github.dushyna.ticketflow.user.dto.request.OrganizationCreateDto;
import io.github.dushyna.ticketflow.user.dto.request.TenantRegistrationDto;
import io.github.dushyna.ticketflow.user.dto.request.UserCreateDto;
import io.github.dushyna.ticketflow.user.dto.response.UserCreateResponseDto;
import io.github.dushyna.ticketflow.user.dto.response.UserResponseDto;
import io.github.dushyna.ticketflow.user.entity.AppUser;
import io.github.dushyna.ticketflow.user.entity.ConfirmationStatus;
import io.github.dushyna.ticketflow.user.entity.Role;
import io.github.dushyna.ticketflow.user.exception.UserAlreadyExistException;
import io.github.dushyna.ticketflow.user.service.interfaces.UserService;
import io.github.dushyna.ticketflow.user.utils.AppUserMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit tests for UserRegisterService")
class UserRegisterServiceImplTest {

    @Mock private EmailService emailService;
    @Mock private BCryptPasswordEncoder passwordEncoder;
    @Mock private ConfirmationService confirmationService;
    @Mock private UserService userService;
    @Mock private OrganizationRepository organizationRepository;
    @Mock private AppUserMapper userMapper;

    @InjectMocks
    private UserRegisterServiceImpl userRegisterService;

    @Test
    @DisplayName("registerCustomer: Success - should create new user and send email")
    void registerCustomer_Success() {
        // Given
        UserCreateDto dto = new UserCreateDto("new@test.com", "Password123");
        AppUser savedUser = new AppUser("encodedPass", "new@test.com");
        UUID userId = UUID.randomUUID();
        ReflectionTestUtils.setField(savedUser, "id", userId);
        savedUser.setRole(Role.ROLE_USER);

        given(userService.getByEmail("new@test.com")).willReturn(Optional.empty());
        given(passwordEncoder.encode(dto.password())).willReturn("encodedPass");
        given(userService.saveOrUpdate(any(AppUser.class))).willReturn(savedUser);
        given(confirmationService.generateConfirmationCode(any(AppUser.class))).willReturn("code123");

        // When
        UserCreateResponseDto response = userRegisterService.registerCustomer(dto);

        // Then
        assertThat(response.email()).isEqualTo("new@test.com");
        assertThat(response.confirmationResent()).isFalse(); // Правильне поле
        assertThat(response.id()).isEqualTo(userId.toString());

        verify(emailService).sendConfirmationEmail("new@test.com", "code123");
    }

    @Test
    @DisplayName("registerCustomer: Resend - should regenerate code for existing unconfirmed user")
    void registerCustomer_ResendCode() {
        // Given
        UserCreateDto dto = new UserCreateDto("unconfirmed@test.com", "Password123");
        AppUser existingUser = new AppUser();
        UUID userId = UUID.randomUUID();
        ReflectionTestUtils.setField(existingUser, "id", userId);
        existingUser.setEmail("unconfirmed@test.com");
        existingUser.setConfirmationStatus(ConfirmationStatus.UNCONFIRMED);
        existingUser.setProvider("LOCAL");
        existingUser.setRole(Role.ROLE_USER);

        given(userService.getByEmail("unconfirmed@test.com")).willReturn(Optional.of(existingUser));
        given(confirmationService.regenerateCode(existingUser)).willReturn("newCode");

        // When
        UserCreateResponseDto response = userRegisterService.registerCustomer(dto);

        // Then
        assertThat(response.confirmationResent()).isTrue();
        assertThat(response.email()).isEqualTo("unconfirmed@test.com");
        verify(emailService).sendConfirmationEmail("unconfirmed@test.com", "newCode");
        verify(userService, never()).saveOrUpdate(any());
    }

    @Test
    @DisplayName("registerCustomer: Failure - throw exception if user is LOCAL and already CONFIRMED")
    void registerCustomer_ThrowsException_IfAlreadyConfirmed() {
        // Given
        UserCreateDto dto = new UserCreateDto("confirmed@test.com", "Password123");
        AppUser existingUser = new AppUser();
        existingUser.setConfirmationStatus(ConfirmationStatus.CONFIRMED);
        existingUser.setProvider("LOCAL");

        given(userService.getByEmail("confirmed@test.com")).willReturn(Optional.of(existingUser));

        // When & Then
        assertThatThrownBy(() -> userRegisterService.registerCustomer(dto))
                .isInstanceOf(UserAlreadyExistException.class);
    }

    @Test
    @DisplayName("registerTenant: Success - should create organization and admin with ROLE_TENANT_ADMIN")
    void registerTenant_Success() {
        // 1. Given
        OrganizationCreateDto orgDto = new OrganizationCreateDto(
                "Cinema World",
                "cinema-world",
                "office@cinemaworld.com"
        );
        UserCreateDto adminDto = new UserCreateDto("owner@test.com", "SecurePass123");
        TenantRegistrationDto registrationDto = new TenantRegistrationDto(adminDto, orgDto);

        Organization savedOrg = Organization.builder()
                .name(orgDto.name())
                .slug(orgDto.slug())
                .contactEmail(orgDto.contactEmail())
                .build();
        ReflectionTestUtils.setField(savedOrg, "id", UUID.randomUUID());

        AppUser savedAdmin = new AppUser("encodedPassword", "owner@test.com");
        ReflectionTestUtils.setField(savedAdmin, "id", UUID.randomUUID());
        savedAdmin.setRole(Role.ROLE_TENANT_ADMIN);
        savedAdmin.setOrganization(savedOrg);

        given(organizationRepository.save(any(Organization.class))).willReturn(savedOrg);
        given(userService.getByEmail(anyString())).willReturn(Optional.empty());
        given(passwordEncoder.encode(adminDto.password())).willReturn("encodedPassword");
        given(userService.saveOrUpdate(any(AppUser.class))).willReturn(savedAdmin);
        given(confirmationService.generateConfirmationCode(any(AppUser.class))).willReturn("admin-confirm-code");

        // 2. When
        UserCreateResponseDto response = userRegisterService.registerTenant(registrationDto);

        // 3. Then
        assertThat(response.email()).isEqualTo("owner@test.com");
        assertThat(response.role()).isEqualTo(Role.ROLE_TENANT_ADMIN.name());

        verify(organizationRepository).save(argThat(org ->
                org.getName().equals("Cinema World") &&
                        org.getSlug().equals("cinema-world")
        ));

        verify(userService).saveOrUpdate(argThat(user ->
                user.getRole() == Role.ROLE_TENANT_ADMIN &&
                        user.getEmail().equals("owner@test.com")
        ));

        verify(emailService).sendConfirmationEmail(eq("owner@test.com"), eq("admin-confirm-code"));
    }

    @Test
    @DisplayName("confirmRegistration: Success - should activate user and map to response DTO")
    void confirmRegistration_Success() {
        // 1. Given
        String code = "valid-activation-code";
        AppUser user = new AppUser();
        user.setEmail("user@test.com");
        user.setConfirmationStatus(ConfirmationStatus.UNCONFIRMED);

        ConfirmationCode confirmationCodeMock = mock(ConfirmationCode.class);
        given(confirmationCodeMock.getUser()).willReturn(user);
        given(confirmationService.getConfirmationIfValidOrThrow(code)).willReturn(confirmationCodeMock);

        UserResponseDto expectedDto = new UserResponseDto(
                UUID.randomUUID().toString(),
                "user@test.com",
                "Homer",
                "Simpson",
                null,
                null,
                "ROLE_USER",
                ConfirmationStatus.CONFIRMED,
                null
        );

        given(userMapper.mapEntityToResponseDto(user)).willReturn(expectedDto);

        // 2. When
        UserResponseDto result = userRegisterService.confirmRegistration(code);

        // 3. Then
        assertThat(user.getConfirmationStatus()).isEqualTo(ConfirmationStatus.CONFIRMED);
        assertThat(result.email()).isEqualTo("user@test.com");
        assertThat(result.confirmationStatus()).isEqualTo(ConfirmationStatus.CONFIRMED);

        verify(userService).saveOrUpdate(user);
        verify(confirmationService).removeToken(confirmationCodeMock);
    }

    @Test
    @DisplayName("registerCustomer: Failure - throw exception if user exists via OAuth2 (GOOGLE)")
    void registerCustomer_ThrowsException_IfRegisteredViaGoogle() {
        // Given
        UserCreateDto dto = new UserCreateDto("google_user@test.com", "Password123");
        AppUser existingUser = new AppUser();
        existingUser.setProvider("GOOGLE");

        given(userService.getByEmail(anyString())).willReturn(Optional.of(existingUser));

        // When & Then
        assertThatThrownBy(() -> userRegisterService.registerCustomer(dto))
                .isInstanceOf(UserAlreadyExistException.class)
                .hasMessageContaining("User already registered via GOOGLE");
    }

    @Test
    @DisplayName("registerCustomer: Normalization - should lowercase and trim email before searching and saving")
    void registerCustomer_ShouldNormalizeEmail() {
        // 1. Given
        String rawEmail = "  Homer.Simpson@SPRINGFIELD.com  ";
        String normalizedEmail = "homer.simpson@springfield.com";

        UserCreateDto dto = new UserCreateDto(rawEmail, "Password123");

        AppUser savedUser = new AppUser("encodedHash", normalizedEmail);
        ReflectionTestUtils.setField(savedUser, "id", UUID.randomUUID());
        savedUser.setRole(Role.ROLE_USER);

        given(userService.getByEmail(normalizedEmail)).willReturn(Optional.empty());
        given(passwordEncoder.encode(anyString())).willReturn("encodedHash");
        given(userService.saveOrUpdate(any(AppUser.class))).willReturn(savedUser);
        given(confirmationService.generateConfirmationCode(any(AppUser.class))).willReturn("code123");

        // 2. When
        UserCreateResponseDto response = userRegisterService.registerCustomer(dto);

        // 3. Then
        verify(userService).getByEmail(normalizedEmail);

        verify(userService).saveOrUpdate(argThat(user ->
                user.getEmail().equals(normalizedEmail)
        ));

        assertThat(response.email()).isEqualTo(normalizedEmail);

        verify(emailService).sendConfirmationEmail(eq(normalizedEmail), anyString());
    }

    @Test
    @DisplayName("registerTenant: Failure - should throw RestApiException when slug is taken")
    void registerTenant_ThrowsRestApiException_WhenSlugIsTaken() {
        // 1. Given
        OrganizationCreateDto orgDto = new OrganizationCreateDto("New Cinema", "cinema-star", "contact@test.com");
        UserCreateDto adminDto = new UserCreateDto("admin@test.com", "Password123");
        TenantRegistrationDto registrationDto = new TenantRegistrationDto(adminDto, orgDto);

        given(organizationRepository.existsBySlug("cinema-star")).willReturn(true);

        // 2. When & Then
        assertThatThrownBy(() -> userRegisterService.registerTenant(registrationDto))
                .isInstanceOf(io.github.dushyna.ticketflow.exception.handling.exceptions.common.RestApiException.class)
                .satisfies(ex -> {
                    var apiEx = (io.github.dushyna.ticketflow.exception.handling.exceptions.common.RestApiException) ex;
                    assertThat(apiEx.getHttpStatus()).isEqualTo(org.springframework.http.HttpStatus.CONFLICT);
                    assertThat(apiEx.getMessage()).isEqualTo("Organization with this slug already exists");
                });

        verify(organizationRepository, never()).save(any());
    }

}
