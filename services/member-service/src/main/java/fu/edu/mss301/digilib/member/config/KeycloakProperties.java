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

    public String tokenUrl() {
        return baseUrl + "/realms/" + realm + "/protocol/openid-connect/token";
    }

    public String revokeUrl() {
        return baseUrl + "/realms/" + realm + "/protocol/openid-connect/revoke";
    }

    public String logoutUrl() {
        return baseUrl + "/realms/" + realm + "/protocol/openid-connect/logout";
    }
}
