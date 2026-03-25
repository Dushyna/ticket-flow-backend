package io.github.dushyna.ticketflow.security.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

/**
 * Request that contains refresh token for getting new access token purpose
 */
@Setter
@Getter
@Schema(description = "Data Transfer Object for delivering refresh tokens")
public class RefreshRequestDto {

    /**
     * Refresh token
     */
    @Schema(description = "Refresh token", example = "eyJhbGciOiJIUzI1NiJ9...0ADS106w")
    private String refreshToken;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        RefreshRequestDto that = (RefreshRequestDto) o;
        return Objects.equals(refreshToken, that.refreshToken);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(refreshToken);
    }

    @Override
    public String toString() {
        return "Refresh token - " + refreshToken;
    }
}
