package io.github.dushyna.ticketflow.security.service;

import io.github.dushyna.ticketflow.security.dto.AuthUserDetails;
import io.github.dushyna.ticketflow.user.entity.AppUser;
import io.github.dushyna.ticketflow.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit tests for CustomUserDetailsService")
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService userDetailsService;

    @Test
    @DisplayName("Should return UserDetails when user exists")
    void loadUserByUsername_Success() {
        // Given
        String email = "test@example.com";
        AppUser user = new AppUser();
        user.setEmail(email);

        given(userRepository.findByEmailWithOrganization(email)).willReturn(Optional.of(user));

        // When
        UserDetails result = userDetailsService.loadUserByUsername(email);

        // Then
        assertThat(result).isInstanceOf(AuthUserDetails.class);
        assertThat(result.getUsername()).isEqualTo(email);
        verify(userRepository).findByEmailWithOrganization(email);
    }

    @Test
    @DisplayName("Should throw UsernameNotFoundException when user does not exist")
    void loadUserByUsername_UserNotFound() {
        // Given
        String email = "unknown@example.com";
        given(userRepository.findByEmailWithOrganization(email)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername(email))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User not found: " + email);
    }

    @Test
    @DisplayName("Should handle null username by throwing exception")
    void loadUserByUsername_NullUsername() {
        // Given
        given(userRepository.findByEmailWithOrganization(null)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername(null))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    @DisplayName("Should call repository with exact same email as provided")
    void loadUserByUsername_PreservesEmailCase() {
        // Given
        String email = "UPPERCASE@test.com";
        given(userRepository.findByEmailWithOrganization(email)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername(email))
                .isInstanceOf(UsernameNotFoundException.class);

        verify(userRepository).findByEmailWithOrganization(email);
    }

}
