package io.github.dushyna.ticketflow.user.service.impl;

import io.github.dushyna.ticketflow.user.dto.request.UpdateUserDetailsDto;
import io.github.dushyna.ticketflow.user.dto.response.UserResponseDto;
import io.github.dushyna.ticketflow.user.entity.AppUser;
import io.github.dushyna.ticketflow.user.exception.UserNotFoundException;
import io.github.dushyna.ticketflow.user.repository.UserRepository;
import io.github.dushyna.ticketflow.user.utils.AppUserMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit tests for UserServiceImpl")
class UserServiceImplTest {

    @Mock private UserRepository repository;
    @Mock private AppUserMapper mappingService;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    @DisplayName("updateUserDetails: should update only non-null fields and normalize names")
    void updateUserDetails_Success() {
        // 1. Given
        AppUser currentUser = new AppUser();
        currentUser.setFirstName("Oldname");
        currentUser.setLastName("Oldsurname");
        currentUser.setPhone("+111");

        UpdateUserDetailsDto dto = new UpdateUserDetailsDto(
                "  homer  ",         // firstName
                null,               // lastName
                LocalDate.of(1990, 5, 15),
                "+380501234567"     // phone
        );

        given(repository.save(any(AppUser.class))).willReturn(currentUser);
        given(mappingService.mapEntityToResponseDto(any(AppUser.class))).willReturn(mock(UserResponseDto.class));

        // 2. When
        userService.updateUserDetails(dto, currentUser);

        // 3. Then
        assertThat(currentUser.getFirstName()).isEqualTo("Homer");
        assertThat(currentUser.getLastName()).isEqualTo("Oldsurname");
        assertThat(currentUser.getPhone()).isEqualTo("+380501234567");
        assertThat(currentUser.getBirthDate()).isEqualTo(LocalDate.of(1990, 5, 15));

        verify(repository).save(currentUser);
        verify(mappingService).mapEntityToResponseDto(currentUser);
    }

    @Test
    @DisplayName("getByEmailOrThrow: Success - should return user")
    void getByEmailOrThrow_Success() {
        String email = "test@test.com";
        AppUser user = new AppUser();
        given(repository.findByEmailIgnoreCase(email)).willReturn(Optional.of(user));

        AppUser result = userService.getByEmailOrThrow(email);

        assertThat(result).isEqualTo(user);
    }

    @Test
    @DisplayName("getByEmailOrThrow: Failure - should throw UserNotFoundException")
    void getByEmailOrThrow_NotFound() {
        given(repository.findByEmailIgnoreCase(any())).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getByEmailOrThrow("none@test.com"))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    @DisplayName("saveOrUpdate: Success")
    void saveOrUpdate_Success() {
        AppUser user = new AppUser();
        given(repository.save(user)).willReturn(user);

        AppUser result = userService.saveOrUpdate(user);

        assertThat(result).isEqualTo(user);
        verify(repository).save(user);
    }

    @Test
    @DisplayName("getByEmail: Found and Not Found")
    void getByEmail_Scenarios() {
        String email = "test@test.com";
        given(repository.findByEmailIgnoreCase(email)).willReturn(Optional.of(new AppUser()));
        assertThat(userService.getByEmail(email)).isPresent();

        given(repository.findByEmailIgnoreCase("none")).willReturn(Optional.empty());
        assertThat(userService.getByEmail("none")).isEmpty();
    }

    @Test
    @DisplayName("getByEmailOrThrow: Success and Exception")
    void getByEmailOrThrow_Scenarios() {
        String email = "test@test.com";
        given(repository.findByEmailIgnoreCase(email)).willReturn(Optional.of(new AppUser()));
        assertThat(userService.getByEmailOrThrow(email)).isNotNull();

        given(repository.findByEmailIgnoreCase("error")).willReturn(Optional.empty());
        assertThatThrownBy(() -> userService.getByEmailOrThrow("error"))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    @DisplayName("getAll: Success and Empty List")
    void getAll_Scenarios() {
        AppUser user = new AppUser();
        given(repository.findAll()).willReturn(List.of(user));
        given(mappingService.mapEntityToResponseDto(user)).willReturn(mock(UserResponseDto.class));

        assertThat(userService.getAll()).hasSize(1);

        given(repository.findAll()).willReturn(List.of());
        assertThat(userService.getAll()).isEmpty();
    }

    @Test
    @DisplayName("getUserDetails: Success")
    void getUserDetails_Success() {
        AppUser user = new AppUser();
        userService.getUserDetails(user);
        verify(mappingService).mapEntityToResponseDto(user);
    }


    @Test
    @DisplayName("updateUserDetails: Edge case all fields null")
    void updateUserDetails_AllNull() {
        AppUser user = new AppUser();
        user.setFirstName("John");
        UpdateUserDetailsDto dto = new UpdateUserDetailsDto(null, null, null, null);

        given(repository.save(user)).willReturn(user);
        userService.updateUserDetails(dto, user);

        assertThat(user.getFirstName()).isEqualTo("John"); // Нічого не змінилось
        verify(repository).save(user);
    }

    @Test
    @DisplayName("updateUserDetails: should handle names with multiple spaces and mixed case")
    void updateUserDetails_ComplexNameNormalization() {
        // Given
        AppUser user = new AppUser();
        UpdateUserDetailsDto dto = new UpdateUserDetailsDto("  jOhN  ", "  sMiTh  ", null, null);

        given(repository.save(user)).willReturn(user);
        given(mappingService.mapEntityToResponseDto(user)).willReturn(mock(UserResponseDto.class));

        // When
        userService.updateUserDetails(dto, user);

        // Then
        assertThat(user.getFirstName()).isEqualTo("John");
        assertThat(user.getLastName()).isEqualTo("Smith");
    }

}
