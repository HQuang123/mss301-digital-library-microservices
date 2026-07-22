package fu.edu.mss301.digilib.member.api.controller;

import fu.edu.mss301.digilib.member.api.dto.MemberResponse;
import fu.edu.mss301.digilib.member.api.dto.auth.LoginRequest;
import fu.edu.mss301.digilib.member.api.dto.auth.LogoutRequest;
import fu.edu.mss301.digilib.member.api.dto.auth.LogoutResponse;
import fu.edu.mss301.digilib.member.api.dto.auth.OAuth2ExchangeRequest;
import fu.edu.mss301.digilib.member.api.dto.auth.ForgotPasswordRequest;
import fu.edu.mss301.digilib.member.api.dto.auth.RegisterRequest;
import fu.edu.mss301.digilib.member.api.dto.auth.TokenResponse;
import fu.edu.mss301.digilib.member.domain.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    /**
     * Register a new member.
     *
     * Creates a Keycloak user, sets the password, and sends a verification email.
     * The account is not usable until the user clicks the verification link.
     *
     * Returns 201 Created with the newly provisioned member profile.
     */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<MemberResponse> register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request)
                .map(MemberResponse::from);
    }

    /**
     * Login with username (email) and password.
     *
     * Returns 200 OK with access_token, refresh_token, and expiry metadata.
     * Use the access_token as a Bearer token for all subsequent authenticated requests.
     */
    @PostMapping("/login")
    public Mono<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    /**
     * Exchange an OAuth2 authorization code for tokens.
     *
     * The frontend sends the authorization code, PKCE code_verifier, and
     * redirect_uri.  The backend adds the confidential client_secret and
     * forwards the exchange to Keycloak.
     *
     * Returns 200 OK with access_token, refresh_token, and expiry metadata.
     */
    @PostMapping("/oauth2/exchange")
    public Mono<TokenResponse> exchangeOAuth2Code(@Valid @RequestBody OAuth2ExchangeRequest request) {
        return authService.exchangeOAuth2Code(request);
    }

    /**
     * Logout the current session.
     *
     * Revokes the access token, refresh token, and Keycloak session, then
     * returns the RP-Initiated Logout URL that the frontend MUST redirect
     * the browser to in order to clear Keycloak's browser cookies.
     *
     * Without this browser redirect, Keycloak will silently reuse the old
     * session on the next login, bypassing identity provider redirects
     * (e.g., Google account chooser).
     *
     * Returns 200 OK with a logout_redirect_url the frontend should
     * redirect the browser to via window.location.href.
     */
    @PostMapping("/logout")
    public Mono<LogoutResponse> logout(
            @Valid @RequestBody LogoutRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        String accessToken = null;
        if (authHeader != null && authHeader.length() > 7 && authHeader.substring(0, 7).equalsIgnoreCase("Bearer ")) {
            accessToken = authHeader.substring(7);
        }
        return authService.logout(accessToken, request.refreshToken(),
                        request.idToken(), request.postLogoutRedirectUri())
                .map(LogoutResponse::new);
    }

    /**
     * Trigger self-service forgot password flow.
     *
     * Sends a password reset email (via Keycloak) to the user's email if registered.
     */
    @PostMapping("/forgot-password")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Mono<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return authService.forgotPassword(request.email());
    }
}
