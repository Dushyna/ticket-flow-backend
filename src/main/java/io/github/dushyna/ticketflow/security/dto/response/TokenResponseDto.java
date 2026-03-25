package io.github.dushyna.ticketflow.security.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

/**
 * Response that contains access and refresh tokens generated for User during login process
 */
@Schema(name = "TokenResponse", description = "Response payload containing access and refresh tokens")
@Setter
@Getter
public class TokenResponseDto {

    @Schema(description = "JWT access token", example = "eyJhbGciOiJIUzI1NiJ9...0ADS106w")
    private String accessToken;

    @Schema(description = "JWT refresh token", example = "eyJhbGciOiJIUzI1NiJ9...joEW3Hv8Yrs")
    private String refreshToken;

    public TokenResponseDto(String accessToken, String refreshToken) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        TokenResponseDto that = (TokenResponseDto) o;
        return Objects.equals(accessToken, that.accessToken) && Objects.equals(refreshToken, that.refreshToken);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accessToken, refreshToken);
    }

    @Override
    public String toString() {
        return String.format("Access token - %s, refresh token - %s.", accessToken, refreshToken);
    }
}
