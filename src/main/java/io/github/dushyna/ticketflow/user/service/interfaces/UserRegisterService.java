package io.github.dushyna.ticketflow.user.service.interfaces;

import io.github.dushyna.ticketflow.user.dto.request.TenantRegistrationDto;
import io.github.dushyna.ticketflow.user.dto.request.UserCreateDto;
import io.github.dushyna.ticketflow.user.dto.response.UserCreateResponseDto;
import io.github.dushyna.ticketflow.user.dto.response.UserResponseDto;

public interface UserRegisterService {

    UserCreateResponseDto registerCustomer(UserCreateDto dto);

    UserCreateResponseDto registerTenant(TenantRegistrationDto dto);

    UserResponseDto confirmRegistration(String code);
}
