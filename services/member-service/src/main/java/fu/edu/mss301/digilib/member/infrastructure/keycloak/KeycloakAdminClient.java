package fu.edu.mss301.digilib.member.infrastructure.keycloak;

import fu.edu.mss301.digilib.member.api.error.ApiException;
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
import java.util.concurrent.ConcurrentHashMap;
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

    // Keycloak realm-role representations (id + attributes) are stable for a
    // realm's lifetime, so a simple unbounded in-process cache keyed by role
    // name is sufficient.  The cache holds the parsed RoleRep so callers that
    // need attributes (e.g. the onboarding flow) don't issue a second GET.
    private final Map<String, RoleRep> cachedRoles = new ConcurrentHashMap<>();

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
                "emailVerified", !kc.isRequireEmailVerification()
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

    public boolean isRequireEmailVerification() {
        return kc.isRequireEmailVerification();
    }

    /**
     * Builds the OIDC RP-Initiated Logout URL that the frontend must redirect
     * the browser to in order to clear Keycloak's browser cookies.
     */
    public String buildRpInitiatedLogoutUrl(String idTokenHint, String postLogoutRedirectUri) {
        return kc.rpInitiatedLogoutUrl(idTokenHint, postLogoutRedirectUri);
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
     * Look up a user in Keycloak by email.
     * @return the Keycloak user-id if found, or Mono.empty() if not.
     */
    public Mono<String> findUserByEmail(String email) {
        return adminToken()
                .flatMap(token -> webClient.get()
                        .uri(kc.adminUsersUrl() + "?email=" + email)
                        .header("Authorization", "Bearer " + token)
                        .retrieve()
                        .bodyToMono(new org.springframework.core.ParameterizedTypeReference<List<Map<String, Object>>>() {})
                        .flatMap(users -> {
                            if (users == null || users.isEmpty()) {
                                return Mono.empty();
                            }
                            return Mono.just(String.valueOf(users.get(0).get("id")));
                        })
                );
    }

    /**
     * Send a password reset email (UPDATE_PASSWORD action) to the Keycloak user.
     */
    public Mono<Void> sendForgotPasswordEmail(String keycloakUserId) {
        return adminToken()
                .flatMap(token -> webClient.put()
                        .uri(kc.adminUserExecuteActionsEmailUrl(keycloakUserId) + "?client_id=" + kc.getClientId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(List.of("UPDATE_PASSWORD"))
                        .retrieve()
                        .toBodilessEntity()
                        .then()
                );
    }

    /**
     * Assign a realm role to a user.  Resolves (and caches) the role's UUID via
     * the Admin REST API, then POSTs the role-mapping.
     *
     * @throws ApiException with code ROLE_NOT_FOUND (HTTP 404) when the role
     *                      does not exist in the realm.
     */
    public Mono<Void> assignRealmRole(String keycloakUserId, String roleName) {
        Mono<Void> call = resolveRoleRep(roleName)
                .flatMap(rep -> adminToken()
                        .flatMap(token -> webClient.post()
                                .uri(kc.adminUserRoleMappingsUrl(keycloakUserId))
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(List.of(Map.of("id", rep.id(), "name", roleName)))
                                .retrieve()
                                .toBodilessEntity()
                                .then()
                        )
                );
        return mapKeycloakAdmin403(call);
    }

    /**
     * Remove a realm role from a user.  Idempotent — Keycloak returns 204 even
     * when the role was not assigned, so callers can use this to clear any
     * stale prior onboarding role without first checking.
     */
    public Mono<Void> removeRealmRole(String keycloakUserId, String roleName) {
        Mono<Void> call = resolveRoleRep(roleName)
                .flatMap(rep -> adminToken()
                        .flatMap(token -> webClient.method(org.springframework.http.HttpMethod.DELETE)
                                .uri(kc.adminUserRoleMappingsUrl(keycloakUserId))
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(List.of(Map.of("id", rep.id(), "name", roleName)))
                                .retrieve()
                                .toBodilessEntity()
                                .then()
                        )
                );
        return mapKeycloakAdmin403(call);
    }

    /**
     * List the realm-role names currently assigned to a user.  Used during
     * onboarding to detect and clear any prior student/lecturer assignment
     * before assigning a new one.
     */
    public Mono<List<String>> listUserRealmRoles(String keycloakUserId) {
        Mono<List<String>> call = adminToken()
                .flatMap(token -> webClient.get()
                        .uri(kc.adminUserRoleMappingsUrl(keycloakUserId))
                        .header("Authorization", "Bearer " + token)
                        .retrieve()
                        .bodyToMono(new org.springframework.core.ParameterizedTypeReference<List<Map<String, Object>>>() {})
                        .map(roles -> roles == null
                                ? List.<String>of()
                                : roles.stream()
                                        .map(role -> String.valueOf(role.get("name")))
                                        .toList())
                );
        return mapKeycloakAdmin403(call);
    }

    /**
     * Fetch a realm role's attributes (e.g. {@code loanPeriodDays},
     * {@code borrowingLimit}, {@code reservationPriority}) as Keycloak stores
     * them: a map of {@code String → List<String>}.  The onboarding flow reads
     * these and applies them to the member profile so that a role's settings
     * actually take effect for the user.
     *
     * <p>The result is cached per role-name for the process lifetime; if an
     * operator edits a role's attributes in the Keycloak Admin Console,
     * restart member-service to pick up the change.
     */
    public Mono<Map<String, List<String>>> fetchRoleAttributes(String roleName) {
        return resolveRoleRep(roleName).map(RoleRep::attributes);
    }

    @SuppressWarnings("unchecked")
    private Mono<RoleRep> resolveRoleRep(String roleName) {
        RoleRep cached = cachedRoles.get(roleName);
        if (cached != null) {
            return Mono.just(cached);
        }
        return adminToken()
                .flatMap(token -> webClient.get()
                        .uri(kc.adminRoleUrl(roleName))
                        .header("Authorization", "Bearer " + token)
                        .retrieve()
                        .bodyToMono(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {})
                        .map(role -> {
                            String id = String.valueOf(role.get("id"));
                            Map<String, List<String>> attributes =
                                    (Map<String, List<String>>) role.getOrDefault("attributes", Map.of());
                            RoleRep rep = new RoleRep(id, attributes);
                            cachedRoles.put(roleName, rep);
                            return rep;
                        })
                )
                .onErrorResume(WebClientResponseException.class, ex -> {
                    HttpStatus status = (HttpStatus) ex.getStatusCode();
                    if (status == HttpStatus.NOT_FOUND) {
                        return Mono.error(new ApiException(
                                HttpStatus.NOT_FOUND,
                                "ROLE_NOT_FOUND",
                                "Role '" + roleName + "' is not configured in the Keycloak realm. "
                                        + "Ask the operator to create realm roles 'student' and 'lecturer' "
                                        + "in the digilib-realm."));
                    }
                    if (status == HttpStatus.FORBIDDEN) {
                        return Mono.error(new ApiException(
                                HttpStatus.SERVICE_UNAVAILABLE,
                                "SERVICE_ACCOUNT_UNAUTHORIZED",
                                "Member service's Keycloak service account cannot read realm roles. "
                                        + "Grant 'realm-management.view-realm' (and 'realm-management.manage-users' "
                                        + "for role assignment) to the digilib-auth service account in the Keycloak "
                                        + "Admin Console under Clients > digilib-auth > Service Accounts Roles."));
                    }
                    return Mono.error(ex);
                });
    }

    /**
     * Wraps an admin REST call so a Keycloak 403 (service account lacks
     * realm-management permissions) surfaces as a structured
     * {@code SERVICE_ACCOUNT_UNAUTHORIZED} error instead of a generic 500.
     */
    private <T> Mono<T> mapKeycloakAdmin403(Mono<T> source) {
        return source.onErrorResume(WebClientResponseException.class, ex -> {
            if (ex.getStatusCode() == HttpStatus.FORBIDDEN) {
                return Mono.error(new ApiException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "SERVICE_ACCOUNT_UNAUTHORIZED",
                        "Member service's Keycloak service account lacks permission to manage realm roles. "
                                + "Grant 'realm-management.view-realm' and 'realm-management.manage-users' "
                                + "to the digilib-auth service account in the Keycloak Admin Console "
                                + "(Clients > digilib-auth > Service Accounts Roles)."));
            }
            return Mono.error(ex);
        });
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
     * Revoke an access token so it can no longer be used.
     */
    public Mono<Void> revokeAccessToken(String accessToken) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", kc.getClientId());
        form.add("client_secret", kc.getClientSecret());
        form.add("token", accessToken);
        form.add("token_type_hint", "access_token");

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

    /**
     * Exchange an OAuth2 authorization code (from the PKCE flow) for tokens.
     * The client_secret is added server-side so it never reaches the browser.
     */
    public Mono<KeycloakTokenResponse> exchangeAuthorizationCode(String code, String codeVerifier, String redirectUri) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", kc.getClientId());
        form.add("client_secret", kc.getClientSecret());
        form.add("code", code);
        form.add("code_verifier", codeVerifier);
        form.add("redirect_uri", redirectUri);

        return webClient.post()
                .uri(kc.tokenUrl())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(form))
                .retrieve()
                .bodyToMono(KeycloakTokenResponse.class);
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

    /**
     * Subset of Keycloak's role representation that we actually use: the role
     * UUID (for role-mapping POST/DELETE bodies) and the role's attributes
     * (operator-defined key→list-of-strings, e.g. for borrower limits).
     */
    record RoleRep(String id, Map<String, List<String>> attributes) {
    }
}
