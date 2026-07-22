package fu.edu.mss301.digilib.member.infrastructure.keycloak;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Maps the JSON body returned by Keycloak's token endpoint.
 */
public record KeycloakTokenResponse(
        @JsonProperty("access_token")  String accessToken,
        @JsonProperty("refresh_token") String refreshToken,
        @JsonProperty("id_token")      String idToken,
        @JsonProperty("expires_in")    long expiresIn,
        @JsonProperty("refresh_expires_in") long refreshExpiresIn,
        @JsonProperty("token_type")    String tokenType,
        @JsonProperty("session_state") String sessionState,
        @JsonProperty("scope")         String scope
) {}
