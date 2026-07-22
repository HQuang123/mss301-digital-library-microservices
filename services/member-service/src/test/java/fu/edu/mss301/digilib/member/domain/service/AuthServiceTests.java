package fu.edu.mss301.digilib.member.domain.service;

import fu.edu.mss301.digilib.member.api.dto.auth.RegisterRequest;
import fu.edu.mss301.digilib.member.api.error.ApiException;
import fu.edu.mss301.digilib.member.infrastructure.keycloak.KeycloakAdminClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTests {

    private static final String KEYCLOAK_ID = "keycloak-user-1";
    private static final RegisterRequest REQUEST = new RegisterRequest(
            "reader@example.com", "SecurePassword123", "Test", "Reader");

    @Mock
    private KeycloakAdminClient keycloakClient;

    @Mock
    private MemberProfileService profileService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(keycloakClient, profileService);
        lenient().when(keycloakClient.isRequireEmailVerification()).thenReturn(true);
    }

    @Test
    void rollsBackKeycloakUserWhenVerificationEmailCannotBeSent() {
        when(keycloakClient.createUser(REQUEST.email(), REQUEST.firstName(), REQUEST.lastName()))
                .thenReturn(Mono.just(KEYCLOAK_ID));
        when(keycloakClient.setPassword(KEYCLOAK_ID, REQUEST.password())).thenReturn(Mono.empty());
        when(keycloakClient.sendVerificationEmail(KEYCLOAK_ID))
                .thenReturn(Mono.error(new IllegalStateException("SMTP unavailable")));
        when(keycloakClient.deleteUser(KEYCLOAK_ID)).thenReturn(Mono.empty());

        StepVerifier.create(authService.register(REQUEST))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(ApiException.class);
                    ApiException apiException = (ApiException) error;
                    assertThat(apiException.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                    assertThat(apiException.getCode()).isEqualTo("VERIFICATION_EMAIL_UNAVAILABLE");
                })
                .verify();

        verify(keycloakClient).deleteUser(KEYCLOAK_ID);
        verifyNoInteractions(profileService);
    }

    @Test
    void keepsOriginalRegistrationErrorWhenRollbackAlsoFails() {
        when(keycloakClient.createUser(REQUEST.email(), REQUEST.firstName(), REQUEST.lastName()))
                .thenReturn(Mono.just(KEYCLOAK_ID));
        when(keycloakClient.setPassword(KEYCLOAK_ID, REQUEST.password())).thenReturn(Mono.empty());
        when(keycloakClient.sendVerificationEmail(KEYCLOAK_ID)).thenReturn(Mono.empty());
        when(profileService.registerOrFetchProfile(
                KEYCLOAK_ID, REQUEST.email(), REQUEST.firstName(), REQUEST.lastName()))
                .thenReturn(Mono.error(new IllegalStateException("database unavailable")));
        when(keycloakClient.deleteUser(KEYCLOAK_ID))
                .thenReturn(Mono.error(new IllegalStateException("Keycloak unavailable")));

        StepVerifier.create(authService.register(REQUEST))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(ApiException.class);
                    ApiException apiException = (ApiException) error;
                    assertThat(apiException.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
                    assertThat(apiException.getCode()).isEqualTo("REGISTRATION_FAILED");
                })
                .verify();

        verify(keycloakClient).deleteUser(KEYCLOAK_ID);
    }

    @Test
    void forgotPasswordSendsEmailWhenUserExists() {
        String email = "test@example.com";
        String userId = "keycloak-user-123";
        when(keycloakClient.findUserByEmail(email)).thenReturn(Mono.just(userId));
        when(keycloakClient.sendForgotPasswordEmail(userId)).thenReturn(Mono.empty());

        StepVerifier.create(authService.forgotPassword(email))
                .verifyComplete();

        verify(keycloakClient).findUserByEmail(email);
        verify(keycloakClient).sendForgotPasswordEmail(userId);
    }

    @Test
    void forgotPasswordSilentlySucceedsWhenUserDoesNotExist() {
        // Always-202 behavior: an unknown email must not surface as an error.
        String email = "nonexistent@example.com";
        when(keycloakClient.findUserByEmail(email)).thenReturn(Mono.empty());

        StepVerifier.create(authService.forgotPassword(email))
                .verifyComplete();

        verify(keycloakClient).findUserByEmail(email);
        verify(keycloakClient, never()).sendForgotPasswordEmail(any());
    }

    @Test
    void forgotPasswordSucceedsEvenWhenKeycloakEmailSendFails() {
        // The endpoint must not leak "email send failed" versus "user not found".
        String email = "registered@example.com";
        String userId = "keycloak-user-456";
        when(keycloakClient.findUserByEmail(email)).thenReturn(Mono.just(userId));
        when(keycloakClient.sendForgotPasswordEmail(userId))
                .thenReturn(Mono.error(new IllegalStateException("SMTP unavailable")));

        StepVerifier.create(authService.forgotPassword(email))
                .verifyComplete();

        verify(keycloakClient).findUserByEmail(email);
    }

    @Test
    void logoutRevokesBothTokensAndInvalidatesSession() {
        String accessToken = "valid-access-token";
        String refreshToken = "valid-refresh-token";
        String idToken = "raw-id-token";
        String postLogoutRedirectUri = "http://localhost:5173";
        String expectedLogoutUrl = "http://localhost:8180/realms/digilib-realm/protocol/openid-connect/logout?client_id=digilib-auth";

        when(keycloakClient.revokeRefreshToken(refreshToken)).thenReturn(Mono.empty());
        when(keycloakClient.revokeAccessToken(accessToken)).thenReturn(Mono.empty());
        when(keycloakClient.logoutSession(refreshToken)).thenReturn(Mono.empty());
        when(keycloakClient.buildRpInitiatedLogoutUrl(idToken, postLogoutRedirectUri)).thenReturn(expectedLogoutUrl);

        StepVerifier.create(authService.logout(accessToken, refreshToken, idToken, postLogoutRedirectUri))
                .expectNext(expectedLogoutUrl)
                .verifyComplete();

        verify(keycloakClient).revokeRefreshToken(refreshToken);
        verify(keycloakClient).revokeAccessToken(accessToken);
        verify(keycloakClient).logoutSession(refreshToken);
        verify(keycloakClient).buildRpInitiatedLogoutUrl(idToken, postLogoutRedirectUri);
    }

    @Test
    void logoutWorksWithoutAccessToken() {
        String refreshToken = "valid-refresh-token";
        String expectedLogoutUrl = "http://localhost:8180/realms/digilib-realm/protocol/openid-connect/logout?client_id=digilib-auth";

        when(keycloakClient.revokeRefreshToken(refreshToken)).thenReturn(Mono.empty());
        when(keycloakClient.logoutSession(refreshToken)).thenReturn(Mono.empty());
        when(keycloakClient.buildRpInitiatedLogoutUrl(null, null)).thenReturn(expectedLogoutUrl);

        StepVerifier.create(authService.logout(null, refreshToken, null, null))
                .expectNext(expectedLogoutUrl)
                .verifyComplete();

        verify(keycloakClient).revokeRefreshToken(refreshToken);
        verify(keycloakClient, never()).revokeAccessToken(any());
        verify(keycloakClient).logoutSession(refreshToken);
    }

    @Test
    void logoutSucceedsEvenIfAccessTokenRevocationFails() {
        String accessToken = "failed-access-token";
        String refreshToken = "valid-refresh-token";
        String expectedLogoutUrl = "http://localhost:8180/realms/digilib-realm/protocol/openid-connect/logout?client_id=digilib-auth";

        when(keycloakClient.revokeRefreshToken(refreshToken)).thenReturn(Mono.empty());
        when(keycloakClient.revokeAccessToken(accessToken))
                .thenReturn(Mono.error(new IllegalStateException("Revocation service unavailable")));
        when(keycloakClient.logoutSession(refreshToken)).thenReturn(Mono.empty());
        when(keycloakClient.buildRpInitiatedLogoutUrl(null, null)).thenReturn(expectedLogoutUrl);

        StepVerifier.create(authService.logout(accessToken, refreshToken, null, null))
                .expectNext(expectedLogoutUrl)
                .verifyComplete();

        verify(keycloakClient).revokeRefreshToken(refreshToken);
        verify(keycloakClient).revokeAccessToken(accessToken);
        verify(keycloakClient).logoutSession(refreshToken);
    }

    @Test
    void logoutSucceedsEvenIfSessionLogoutFails() {
        String accessToken = "valid-access-token";
        String refreshToken = "valid-refresh-token";
        String expectedLogoutUrl = "http://localhost:8180/realms/digilib-realm/protocol/openid-connect/logout?client_id=digilib-auth";

        when(keycloakClient.revokeRefreshToken(refreshToken)).thenReturn(Mono.empty());
        when(keycloakClient.revokeAccessToken(accessToken)).thenReturn(Mono.empty());
        when(keycloakClient.logoutSession(refreshToken))
                .thenReturn(Mono.error(new IllegalStateException("Session logout failed")));
        when(keycloakClient.buildRpInitiatedLogoutUrl(null, null)).thenReturn(expectedLogoutUrl);

        StepVerifier.create(authService.logout(accessToken, refreshToken, null, null))
                .expectNext(expectedLogoutUrl)
                .verifyComplete();

        verify(keycloakClient).revokeRefreshToken(refreshToken);
        verify(keycloakClient).revokeAccessToken(accessToken);
        verify(keycloakClient).logoutSession(refreshToken);
    }

    @Test
    void registerBypassesVerificationEmailWhenDisabled() {
        when(keycloakClient.isRequireEmailVerification()).thenReturn(false);
        when(keycloakClient.createUser(REQUEST.email(), REQUEST.firstName(), REQUEST.lastName()))
                .thenReturn(Mono.just(KEYCLOAK_ID));
        when(keycloakClient.setPassword(KEYCLOAK_ID, REQUEST.password())).thenReturn(Mono.empty());

        fu.edu.mss301.digilib.member.domain.entity.MemberProfile mockProfile =
                new fu.edu.mss301.digilib.member.domain.entity.MemberProfile();
        when(profileService.registerOrFetchProfile(
                KEYCLOAK_ID, REQUEST.email(), REQUEST.firstName(), REQUEST.lastName()))
                .thenReturn(Mono.just(mockProfile));

        StepVerifier.create(authService.register(REQUEST))
                .expectNext(mockProfile)
                .verifyComplete();

        verify(keycloakClient, never()).sendVerificationEmail(any());
    }
}
