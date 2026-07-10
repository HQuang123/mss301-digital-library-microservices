package fu.edu.mss301.digilib.member.api.controller;

import fu.edu.mss301.digilib.member.api.dto.MemberResponse;
import fu.edu.mss301.digilib.member.api.dto.auth.LoginRequest;
import fu.edu.mss301.digilib.member.api.dto.auth.LogoutRequest;
import fu.edu.mss301.digilib.member.api.dto.auth.OAuth2ExchangeRequest;
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
     * Requires a valid JWT in the Authorization header AND the refresh_token in
     * the request body. Both the refresh token and the Keycloak session are
     * invalidated, preventing any further token renewal.
     *
     * Returns 204 No Content on success.
     */

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> logout(@Valid @RequestBody LogoutRequest request) {
        return authService.logout(request.refreshToken());
    }
}
