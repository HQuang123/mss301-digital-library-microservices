package fu.edu.mss301.digilib.member.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Configuration
@ConfigurationProperties(prefix = "keycloak")
@Validated
@Data
public class KeycloakProperties {

    /** Base URL of the Keycloak server, supplied per environment. */
    @NotBlank
    private String baseUrl;

    /** The Keycloak realm this service belongs to */
    @NotBlank
    private String realm;

    /** Canonical OIDC issuer used by Spring Security for token validation. */
    @NotBlank
    private String issuerUri;

    /** The client ID of this service's confidential Keycloak client */
    @NotBlank
    private String clientId;

    /** Rotated client secret injected at runtime; never stored in source control. */
    @NotBlank
    private String clientSecret;

    /** Toggle to require email verification upon registration (useful to disable in local dev) */
    private boolean requireEmailVerification = true;

    // -------------------------------------------------------------------------
    // Derived URL helpers — avoids string formatting scattered across adapters
    // -------------------------------------------------------------------------

    public String adminUsersUrl() {
        return baseUrl + "/admin/realms/" + realm + "/users";
    }

    public String adminUserUrl(String userId) {
        return adminUsersUrl() + "/" + userId;
    }

    public String adminUserResetPasswordUrl(String userId) {
        return adminUserUrl(userId) + "/reset-password";
    }

    public String adminUserSendVerifyEmailUrl(String userId) {
        return adminUserUrl(userId) + "/send-verify-email";
    }

    public String adminUserExecuteActionsEmailUrl(String userId) {
        return adminUserUrl(userId) + "/execute-actions-email";
    }

    /** All realm roles: GET {base}/admin/realms/{realm}/roles */
    public String adminRolesUrl() {
        return baseUrl + "/admin/realms/" + realm + "/roles";
    }

    /** A specific realm role: GET/PUT {base}/admin/realms/{realm}/roles/{roleName} */
    public String adminRoleUrl(String roleName) {
        return adminRolesUrl() + "/" + java.net.URLEncoder.encode(roleName, java.nio.charset.StandardCharsets.UTF_8);
    }

    /** A user's realm role mappings: GET/POST/DELETE {base}/admin/realms/{realm}/users/{id}/role-mappings/realm */
    public String adminUserRoleMappingsUrl(String userId) {
        return adminUserUrl(userId) + "/role-mappings/realm";
    }

    public String tokenUrl() {
        return baseUrl + "/realms/" + realm + "/protocol/openid-connect/token";
    }

    public String revokeUrl() {
        return baseUrl + "/realms/" + realm + "/protocol/openid-connect/revoke";
    }

    public String logoutUrl() {
        return baseUrl + "/realms/" + realm + "/protocol/openid-connect/logout";
    }

    /**
     * Builds the OIDC RP-Initiated Logout URL that the frontend must redirect
     * the browser to in order to clear Keycloak's browser cookies.
     *
     * @param idTokenHint         the raw id_token from login (may be null)
     * @param postLogoutRedirect  the URI to redirect to after logout (may be null)
     */
    public String rpInitiatedLogoutUrl(String idTokenHint, String postLogoutRedirect) {
        StringBuilder sb = new StringBuilder(logoutUrl());
        sb.append("?client_id=").append(java.net.URLEncoder.encode(clientId, java.nio.charset.StandardCharsets.UTF_8));
        if (idTokenHint != null && !idTokenHint.isBlank()) {
            sb.append("&id_token_hint=").append(java.net.URLEncoder.encode(idTokenHint, java.nio.charset.StandardCharsets.UTF_8));
        }
        if (postLogoutRedirect != null && !postLogoutRedirect.isBlank()) {
            sb.append("&post_logout_redirect_uri=").append(java.net.URLEncoder.encode(postLogoutRedirect, java.nio.charset.StandardCharsets.UTF_8));
        }
        return sb.toString();
    }
}
