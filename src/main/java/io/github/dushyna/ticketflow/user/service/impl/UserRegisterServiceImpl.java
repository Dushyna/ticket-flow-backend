package io.github.dushyna.ticketflow.user.service.impl;

import io.github.dushyna.ticketflow.mail.EmailService;
import io.github.dushyna.ticketflow.mail.confirmation.code.ConfirmationCode;
import io.github.dushyna.ticketflow.mail.confirmation.code.interfaces.ConfirmationService;
import io.github.dushyna.ticketflow.organization.entity.Organization;
import io.github.dushyna.ticketflow.organization.repository.OrganizationRepository;
import io.github.dushyna.ticketflow.user.dto.request.TenantRegistrationDto;
import io.github.dushyna.ticketflow.user.dto.request.UserCreateDto;
import io.github.dushyna.ticketflow.user.dto.response.UserCreateResponseDto;
import io.github.dushyna.ticketflow.user.dto.response.UserResponseDto;
import io.github.dushyna.ticketflow.user.entity.AppUser;
import io.github.dushyna.ticketflow.user.entity.Role;
import io.github.dushyna.ticketflow.user.exception.UserAlreadyExistException;
import io.github.dushyna.ticketflow.user.service.interfaces.UserRegisterService;
import io.github.dushyna.ticketflow.user.service.interfaces.UserService;
import io.github.dushyna.ticketflow.user.util.AppUserMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

import static io.github.dushyna.ticketflow.user.entity.ConfirmationStatus.CONFIRMED;
import static io.github.dushyna.ticketflow.user.entity.ConfirmationStatus.UNCONFIRMED;

@Service
@RequiredArgsConstructor
public class UserRegisterServiceImpl implements UserRegisterService {

    private final EmailService emailService;
    private final BCryptPasswordEncoder passwordEncoder;
    private final ConfirmationService confirmationService;
    private final UserService userService;
    private final OrganizationRepository organizationRepository;
    private final AppUserMapper userMapper;

    @Override
    @Transactional
    public UserCreateResponseDto registerCustomer(final UserCreateDto dto) {
        return processRegistration(dto, null, Role.ROLE_USER);
    }

    @Override
    @Transactional
    public UserCreateResponseDto registerTenant(final TenantRegistrationDto dto) {
        Organization org = Organization.builder()
                .name(dto.organization().name())
                .slug(dto.organization().slug())
                .contactEmail(dto.organization().contactEmail())
                .build();
        org = organizationRepository.save(org);

        return processRegistration(dto.admin(), org, Role.ROLE_TENANT_ADMIN);
    }

    private UserCreateResponseDto processRegistration(UserCreateDto dto, Organization org, Role role) {
        final String normalizedEmail = dto.email().toLowerCase().trim();
        final Optional<AppUser> foundUserByEmail = userService.getByEmail(normalizedEmail);

        if (foundUserByEmail.isPresent()) {
            AppUser existingUser = foundUserByEmail.get();

          if (!"LOCAL".equals(existingUser.getProvider())) {
                throw new UserAlreadyExistException("User already registered via " + existingUser.getProvider());
            }
            return handleExistingUser(existingUser);
        }


        final AppUser appUser = new AppUser(passwordEncoder.encode(dto.password()), normalizedEmail);
        appUser.setRole(role);
        appUser.setOrganization(org);
        appUser.setProvider("LOCAL");

        final AppUser savedNewUser = userService.saveOrUpdate(appUser);
        String confirmationCode = confirmationService.generateConfirmationCode(savedNewUser);
        emailService.sendConfirmationEmail(savedNewUser.getEmail(), confirmationCode);

        return new UserCreateResponseDto(
                savedNewUser.getId().toString(),
                savedNewUser.getEmail(),
                savedNewUser.getRole().name(),
                false
        );
    }

    private UserCreateResponseDto handleExistingUser(AppUser existingUser) {
        if (UNCONFIRMED.equals(existingUser.getConfirmationStatus())) {
            String confirmationCode = confirmationService.regenerateCode(existingUser);
            emailService.sendConfirmationEmail(existingUser.getEmail(), confirmationCode);
            return new UserCreateResponseDto(
                    existingUser.getId().toString(),
                    existingUser.getEmail(),
                    existingUser.getRole().name(),
                    true);
        }
        throw new UserAlreadyExistException();
    }

    @Override
    @Transactional
    public UserResponseDto confirmRegistration(final String code) {
        final ConfirmationCode confirmationToken = confirmationService.getConfirmationIfValidOrThrow(code);
        final AppUser registeredUser = confirmationToken.getUser();

        registeredUser.setConfirmationStatus(CONFIRMED);
        userService.saveOrUpdate(registeredUser);
        confirmationService.removeToken(confirmationToken);

        return userMapper.mapEntityToResponseDto(registeredUser);
    }
}
