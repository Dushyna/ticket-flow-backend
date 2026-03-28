package io.github.dushyna.ticketflow.user.utils;

import io.github.dushyna.ticketflow.user.dto.EmployeeDto;
import io.github.dushyna.ticketflow.user.dto.response.UserResponseDto;
import io.github.dushyna.ticketflow.user.entity.AppUser;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface AppUserMapper {

    // Mapping for general User profile
    @Mapping(target = "organizationId", source = "organization.id")
    @Mapping(target = "role", source = "role")
    UserResponseDto mapEntityToResponseDto(AppUser entity);

    // Mapping for Admin panel (Employees)
    @Mapping(target = "organizationId", source = "organization.id")
    EmployeeDto mapEntityToEmployeeDto(AppUser entity);
}
