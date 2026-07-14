package fu.edu.mss301.digilib.member.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "keycloak")
@Data
public class KeycloakProperties {

    /** Base URL of the Keycloak server, e.g. https://keycloak.huynq.space */
    private String baseUrl;

    /** The Keycloak realm this service belongs to */
    private String realm;

    /** The client ID of this service's confidential Keycloak client */
    private String clientId;

    /** The client secret for service-account token exchange */
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
