package fu.edu.mss301.digilib.member.infrastructure.keycloak;

import fu.edu.mss301.digilib.member.config.KeycloakProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Reactive Keycloak HTTP client.
 *
 * Wraps all Keycloak Admin REST API and OIDC endpoint calls needed for the
 * auth flow.  Uses WebClient (non-blocking) so it integrates cleanly with
 * the rest of the WebFlux pipeline.
 *
 * Admin operations require a service-account access token obtained via the
 * client_credentials grant.  The token is cached in-process and refreshed
 * automatically when it is within 30 seconds of expiry.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KeycloakAdminClient {

    private final KeycloakProperties kc;
    private final WebClient webClient;

    // ── Simple in-memory cache for the service-account token ─────────────────
    private final AtomicReference<CachedToken> cachedAdminToken = new AtomicReference<>();

    // =========================================================================
    // Public API
    // =========================================================================

    /**
     * Create a new user in Keycloak.
     *
     * @return the Keycloak user-id extracted from the Location header.
     */
    public Mono<String> createUser(String email, String firstName, String lastName) {
        Map<String, Object> body = Map.of(
                "username", email,
                "email", email,
                "firstName", firstName != null ? firstName : "",
                "lastName", lastName != null ? lastName : "",
                "enabled", true,
                "emailVerified", false
        );

        return adminToken()
                .flatMap(token -> webClient.post()
                        .uri(kc.adminUsersUrl())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(body)
                        .retrieve()
                        .toBodilessEntity()
                        .map(response -> {
                            // Keycloak returns 201 with Location: .../users/{id}
                            String location = String.valueOf(response.getHeaders().getLocation());
                            return location.substring(location.lastIndexOf('/') + 1);
                        })
                );
    }

    /**
     * Set a permanent (non-temporary) password for an existing Keycloak user.
     */
    public Mono<Void> setPassword(String keycloakUserId, String password) {
        Map<String, Object> body = Map.of(
                "type", "password",
                "value", password,
                "temporary", false
        );

        return adminToken()
                .flatMap(token -> webClient.put()
                        .uri(kc.adminUserResetPasswordUrl(keycloakUserId))
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(body)
                        .retrieve()
                        .toBodilessEntity()
                        .then()
                );
    }

    /**
     * Send a Keycloak verification email to the user. The user must click the
     * link before their account is considered verified.
     */
    public Mono<Void> sendVerificationEmail(String keycloakUserId) {
        return adminToken()
                .flatMap(token -> webClient.put()
                        .uri(kc.adminUserSendVerifyEmailUrl(keycloakUserId))
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .retrieve()
                        .toBodilessEntity()
                        .then()
                );
    }

    /**
     * Delete a Keycloak user.  Called as a compensating action when profile
     * creation fails after the Keycloak user was already created.
     */
    public Mono<Void> deleteUser(String keycloakUserId) {
        return adminToken()
                .flatMap(token -> webClient.delete()
                        .uri(kc.adminUserUrl(keycloakUserId))
                        .header("Authorization", "Bearer " + token)
                        .retrieve()
                        .toBodilessEntity()
                        .then()
                );
    }

    /**
     * Exchange username + password for a Keycloak access + refresh token pair.
     * Uses the Resource Owner Password Credentials (ROPC) grant.
     */
    public Mono<KeycloakTokenResponse> getToken(String username, String password) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id", kc.getClientId());
        form.add("client_secret", kc.getClientSecret());
        form.add("username", username);
        form.add("password", password);
        form.add("scope", "openid profile email");

        return webClient.post()
                .uri(kc.tokenUrl())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(form))
                .retrieve()
                .bodyToMono(KeycloakTokenResponse.class);
    }

    /**
     * Revoke a refresh token so it can no longer be used to obtain new access
     * tokens.
     */
    public Mono<Void> revokeRefreshToken(String refreshToken) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", kc.getClientId());
        form.add("client_secret", kc.getClientSecret());
        form.add("token", refreshToken);
        form.add("token_type_hint", "refresh_token");

        return webClient.post()
                .uri(kc.revokeUrl())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(form))
                .retrieve()
                .toBodilessEntity()
                .then();
    }

    /**
     * Backchannel logout: invalidates the Keycloak session entirely, which
     * also invalidates all tokens issued for that session across all clients.
     */
    public Mono<Void> logoutSession(String refreshToken) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", kc.getClientId());
        form.add("client_secret", kc.getClientSecret());
        form.add("refresh_token", refreshToken);

        return webClient.post()
                .uri(kc.logoutUrl())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(form))
                .retrieve()
                .toBodilessEntity()
                .then();
    }

    // =========================================================================
    // Admin token management (client_credentials grant with in-process cache)
    // =========================================================================

    private Mono<String> adminToken() {
        CachedToken cached = cachedAdminToken.get();
        if (cached != null && cached.isValid()) {
            return Mono.just(cached.accessToken());
        }
        return fetchAdminToken();
    }

    private Mono<String> fetchAdminToken() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", kc.getClientId());
        form.add("client_secret", kc.getClientSecret());

        return webClient.post()
                .uri(kc.tokenUrl())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(form))
                .retrieve()
                .bodyToMono(KeycloakTokenResponse.class)
                .map(response -> {
                    // Cache the token with a 30-second safety margin
                    Instant expiry = Instant.now().plusSeconds(response.expiresIn() - 30);
                    cachedAdminToken.set(new CachedToken(response.accessToken(), expiry));
                    log.debug("Refreshed Keycloak admin service-account token.");
                    return response.accessToken();
                });
    }

    // ── Inner types ───────────────────────────────────────────────────────────

    private record CachedToken(String accessToken, Instant expiresAt) {
        boolean isValid() {
            return Instant.now().isBefore(expiresAt);
        }
    }
}
