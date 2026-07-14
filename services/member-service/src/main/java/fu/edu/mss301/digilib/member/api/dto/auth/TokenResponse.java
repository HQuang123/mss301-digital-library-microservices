package fu.edu.mss301.digilib.member.api.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import fu.edu.mss301.digilib.member.infrastructure.keycloak.KeycloakTokenResponse;

/**
 * Outbound token response returned to the client.
 * Deliberately does NOT expose session_state or internal Keycloak details.
 */
public record TokenResponse(
        @JsonProperty("access_token")  String accessToken,
        @JsonProperty("refresh_token") String refreshToken,
        @JsonProperty("expires_in")    long expiresIn,
        @JsonProperty("refresh_expires_in") long refreshExpiresIn,
        @JsonProperty("token_type")    String tokenType
) {
    public static TokenResponse from(KeycloakTokenResponse kcResponse) {
        return new TokenResponse(
                kcResponse.accessToken(),
                kcResponse.refreshToken(),
                kcResponse.expiresIn(),
                kcResponse.refreshExpiresIn(),
                kcResponse.tokenType()
        );
    }
}
