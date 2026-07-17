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
}
