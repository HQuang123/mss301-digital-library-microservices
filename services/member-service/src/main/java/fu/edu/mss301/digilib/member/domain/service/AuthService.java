package fu.edu.mss301.digilib.member.domain.service;

import fu.edu.mss301.digilib.member.api.dto.auth.LoginRequest;
import fu.edu.mss301.digilib.member.api.dto.auth.OAuth2ExchangeRequest;
import fu.edu.mss301.digilib.member.api.dto.auth.RegisterRequest;
import fu.edu.mss301.digilib.member.api.dto.auth.TokenResponse;
import fu.edu.mss301.digilib.member.domain.entity.MemberProfile;
import fu.edu.mss301.digilib.member.infrastructure.keycloak.KeycloakAdminClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

/**
 * Orchestrates register / login / logout flows.
 *
 * Design notes:
 *  - Register follows a two-step saga: (1) create Keycloak user, (2) create DB
 *    profile.  If step 2 fails, step 1 is compensated (user deleted from Keycloak).
 *  - Login maps every known Keycloak error code to an appropriate HTTP status so
 *    the client never sees a raw Keycloak error body.
 *  - Logout calls both /revoke (refresh token) AND /logout (backchannel session
 *    invalidation) so all tokens for the session are invalidated immediately.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final KeycloakAdminClient keycloakClient;
    private final MemberProfileService profileService;

    // =========================================================================
    // Register
    // =========================================================================

    /**
     * Registers a new user end-to-end:
     *  1. Creates the Keycloak user account.
     *  2. Sets the password.
     *  3. Sends a verification email — user must verify before logging in.
     *  4. Auto-provisions a member profile in the database.
     *
     * If the DB save fails after Keycloak user creation, the Keycloak user is
     * deleted as a compensating action to avoid orphaned identities.
     */
    public Mono<MemberProfile> register(RegisterRequest request) {
        return keycloakClient
                // ── Step 1: create the Keycloak user ─────────────────────────
                .createUser(request.email(), request.firstName(), request.lastName())
                .onErrorMap(WebClientResponseException.class, ex -> mapKeycloakRegisterError(ex, request.email()))

                // ── Step 2: set the password ──────────────────────────────────
                .flatMap(keycloakId -> keycloakClient
                        .setPassword(keycloakId, request.password())
                        .onErrorMap(WebClientResponseException.class, ex -> mapPasswordPolicyError(ex))
                        .thenReturn(keycloakId)
                )

                // ── Step 3: send verification email ──────────────────────────
                .flatMap(keycloakId -> keycloakClient
                        .sendVerificationEmail(keycloakId)
                        .doOnError(e -> log.warn("Failed to send verification email for {}: {}", keycloakId, e.getMessage()))
                        // Non-fatal: don't block registration if email dispatch fails
                        .onErrorResume(e -> Mono.empty())
                        .thenReturn(keycloakId)
                )

                // ── Step 4: create member profile (with rollback on failure) ──
                .flatMap(keycloakId -> profileService
                        .registerOrFetchProfile(keycloakId, request.email(), request.firstName(), request.lastName())
                        .onErrorResume(dbError -> {
                            log.error("DB profile creation failed for keycloak user {}; rolling back.", keycloakId, dbError);
                            return keycloakClient.deleteUser(keycloakId)
                                    .doOnSuccess(v -> log.info("Rolled back Keycloak user {}", keycloakId))
                                    .doOnError(rollbackError -> log.error("Rollback failed for {}: {}", keycloakId, rollbackError.getMessage()))
                                    .then(Mono.error(new ResponseStatusException(
                                            HttpStatus.INTERNAL_SERVER_ERROR,
                                            "Registration failed. Please try again."
                                    )));
                        })
                );
    }

    // =========================================================================
    // Login
    // =========================================================================

    /**
     * Exchanges credentials for an access + refresh token pair.
     * All known Keycloak error shapes are mapped to structured HTTP responses.
     */
    public Mono<TokenResponse> login(LoginRequest request) {
        return keycloakClient
                .getToken(request.username(), request.password())
                .map(TokenResponse::from)
                .onErrorMap(WebClientResponseException.class, this::mapKeycloakLoginError);
    }

    // =========================================================================
    // OAuth2 Authorization Code Exchange
    // =========================================================================

    /**
     * Exchanges an OAuth2 authorization code (from the PKCE redirect flow) for
     * an access + refresh token pair.  The client_secret is added server-side
     * so it never reaches the browser.
     */
    public Mono<TokenResponse> exchangeOAuth2Code(OAuth2ExchangeRequest request) {
        return keycloakClient
                .exchangeAuthorizationCode(request.code(), request.codeVerifier(), request.redirectUri())
                .map(TokenResponse::from)
                .onErrorMap(WebClientResponseException.class, this::mapKeycloakLoginError);
    }

    // =========================================================================
    // Logout
    // =========================================================================

    /**
     * Full logout:
     *  1. Revokes the refresh token so it cannot be used for silent renewal.
     *  2. Performs a backchannel session logout which invalidates the Keycloak
     *     session and all access tokens issued for it.
     *
     * Both steps are attempted regardless of each other's outcome.
     */
    public Mono<Void> logout(String refreshToken) {
        Mono<Void> revoke  = keycloakClient.revokeRefreshToken(refreshToken)
                .onErrorMap(WebClientResponseException.class, this::mapKeycloakLogoutError);

        Mono<Void> session = keycloakClient.logoutSession(refreshToken)
                .onErrorResume(e -> {
                    // Session logout is best-effort; log but don't fail the request
                    log.warn("Backchannel session logout failed: {}", e.getMessage());
                    return Mono.empty();
                });

        // Execute revoke first, then session invalidation
        return revoke.then(session);
    }

    // =========================================================================
    // Error mapping helpers
    // =========================================================================

    private Throwable mapKeycloakRegisterError(WebClientResponseException ex, String email) {
        if (ex.getStatusCode() == HttpStatus.CONFLICT) {
            return new ResponseStatusException(HttpStatus.CONFLICT, "Email '" + email + "' is already registered.");
        }
        log.error("Keycloak user creation failed with {}: {}", ex.getStatusCode(), ex.getResponseBodyAsString());
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Identity service error. Please try again.");
    }

    private Throwable mapPasswordPolicyError(WebClientResponseException ex) {
        if (ex.getStatusCode() == HttpStatus.BAD_REQUEST) {
            // Keycloak returns a human-readable policy message in the response body
            String kcMessage = ex.getResponseBodyAsString();
            // Extract the message field if present, otherwise use a default
            if (kcMessage.contains("message")) {
                return new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Password does not meet the security policy requirements.");
            }
        }
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid password.");
    }

    private Throwable mapKeycloakLoginError(WebClientResponseException ex) {
        String body = ex.getResponseBodyAsString();
        log.debug("Keycloak login error {}: {}", ex.getStatusCode(), body);

        if (ex.getStatusCode() == HttpStatus.UNAUTHORIZED || ex.getStatusCode() == HttpStatus.BAD_REQUEST) {
            // Check for specific Keycloak error codes in the JSON body
            if (body.contains("Account is not fully set up")) {
                return new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Your account setup is not complete. Please check your email for a verification link.");
            }
            if (body.contains("Account disabled")) {
                return new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Your account has been disabled. Please contact support.");
            }
            if (body.contains("invalid_grant")) {
                return new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Invalid username or password.");
            }
            if (body.contains("Account is not fully set up") || body.contains("email_not_verified")) {
                return new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Please verify your email address before logging in.");
            }
        }

        if (ex.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
            return new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many failed attempts. Please wait before trying again.");
        }

        log.error("Unexpected Keycloak login error {}: {}", ex.getStatusCode(), body);
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "Authentication service is temporarily unavailable.");
    }

    private Throwable mapKeycloakLogoutError(WebClientResponseException ex) {
        if (ex.getStatusCode() == HttpStatus.BAD_REQUEST) {
            return new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "The provided refresh token is invalid or has already expired.");
        }
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "Logout service is temporarily unavailable.");
    }
}
