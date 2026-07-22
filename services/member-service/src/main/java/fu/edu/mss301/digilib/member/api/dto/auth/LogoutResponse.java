package fu.edu.mss301.digilib.member.api.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Returned after backchannel token revocation completes.
 *
 * Contains the Keycloak RP-Initiated Logout URL that the frontend
 * MUST redirect the browser to in order to clear Keycloak's browser
 * cookies (AUTH_SESSION_ID, KEYCLOAK_SESSION, KEYCLOAK_IDENTITY).
 *
 * Without this browser redirect, Keycloak will silently reuse the
 * old browser session on the next login attempt, bypassing any
 * identity provider redirects (e.g., Google account chooser).
 */
public record LogoutResponse(

        @JsonProperty("logout_redirect_url")
        String logoutRedirectUrl
) {}
