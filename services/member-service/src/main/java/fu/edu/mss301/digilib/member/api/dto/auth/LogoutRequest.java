package fu.edu.mss301.digilib.member.api.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record LogoutRequest(

        @NotBlank(message = "Refresh token is required")
        @JsonProperty("refresh_token")
        String refreshToken,

        /** The raw id_token from login — needed for RP-initiated logout without a confirmation screen. */
        @JsonProperty("id_token")
        String idToken,

        /** Where to redirect the browser after Keycloak clears its cookies. Must be registered in Keycloak. */
        @JsonProperty("post_logout_redirect_uri")
        String postLogoutRedirectUri
) {}
