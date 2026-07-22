package fu.edu.mss301.digilib.member.domain.service;

import fu.edu.mss301.digilib.member.api.error.ApiException;
import fu.edu.mss301.digilib.member.domain.entity.MemberProfile;
import fu.edu.mss301.digilib.member.domain.repository.MemberProfileRepository;
import fu.edu.mss301.digilib.member.infrastructure.keycloak.KeycloakAdminClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberProfileServiceRoleTests {

    private static final String KEYCLOAK_ID = "keycloak-user-1";
    private static final Map<String, List<String>> STUDENT_ATTRIBUTES = Map.of(
            "borrowingLimit", List.of("5"),
            "loanPeriodDays", List.of("14"),
            "reservationPriority", List.of("1"));
    private static final Map<String, List<String>> LECTURER_ATTRIBUTES = Map.of(
            "borrowingLimit", List.of("10"),
            "loanPeriodDays", List.of("30"),
            "reservationPriority", List.of("3"));

    @Mock
    private MemberProfileRepository repository;
    @Mock
    private KeycloakAdminClient keycloakClient;

    private MemberProfileService service;

    @BeforeEach
    void setUp() {
        service = new MemberProfileService(repository, keycloakClient);
    }

    @Test
    void rejectsInvalidRoleName() {
        StepVerifier.create(service.assignUserRole(KEYCLOAK_ID, "librarian"))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(ApiException.class);
                    ApiException apiException = (ApiException) error;
                    assertThat(apiException.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(apiException.getCode()).isEqualTo("INVALID_ROLE");
                })
                .verify();

        verify(keycloakClient, never()).listUserRealmRoles(any());
        verify(keycloakClient, never()).assignRealmRole(any(), any());
    }

    @Test
    void appliesRoleAttributesToProfileOnStudentOnboarding() {
        when(keycloakClient.listUserRealmRoles(KEYCLOAK_ID)).thenReturn(Mono.just(List.of()));
        when(keycloakClient.assignRealmRole(KEYCLOAK_ID, "student")).thenReturn(Mono.empty());
        when(keycloakClient.fetchRoleAttributes("student")).thenReturn(Mono.just(STUDENT_ATTRIBUTES));

        MemberProfile existing = MemberProfile.builder()
                .id(KEYCLOAK_ID)
                .memberType("READER")
                .borrowingLimit(0)
                .loanPeriodDays(0)
                .reservationPriority(0)
                .createdAt(Instant.parse("2026-01-01T00:00:00Z"))
                .build();
        when(repository.findById(KEYCLOAK_ID)).thenReturn(Mono.just(existing));
        when(repository.save(any(MemberProfile.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(service.assignUserRole(KEYCLOAK_ID, "student"))
                .assertNext(profile -> {
                    assertThat(profile.getMemberType()).isEqualTo("STUDENT");
                    assertThat(profile.getBorrowingLimit()).isEqualTo(5);
                    assertThat(profile.getLoanPeriodDays()).isEqualTo(14);
                    assertThat(profile.getReservationPriority()).isEqualTo(1);
                })
                .verifyComplete();

        verify(keycloakClient).assignRealmRole(KEYCLOAK_ID, "student");
        verify(keycloakClient).fetchRoleAttributes("student");
        verify(keycloakClient, never()).removeRealmRole(any(), any());
    }

    @Test
    void appliesLecturerAttributesWhenSwappingFromStudent() {
        when(keycloakClient.listUserRealmRoles(KEYCLOAK_ID)).thenReturn(Mono.just(List.of("student", "offline_access")));
        when(keycloakClient.removeRealmRole(KEYCLOAK_ID, "student")).thenReturn(Mono.empty());
        when(keycloakClient.assignRealmRole(KEYCLOAK_ID, "lecturer")).thenReturn(Mono.empty());
        when(keycloakClient.fetchRoleAttributes("lecturer")).thenReturn(Mono.just(LECTURER_ATTRIBUTES));

        MemberProfile existing = MemberProfile.builder()
                .id(KEYCLOAK_ID)
                .memberType("STUDENT")
                .borrowingLimit(5)
                .loanPeriodDays(14)
                .reservationPriority(1)
                .createdAt(Instant.parse("2026-01-01T00:00:00Z"))
                .build();
        when(repository.findById(KEYCLOAK_ID)).thenReturn(Mono.just(existing));
        when(repository.save(any(MemberProfile.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(service.assignUserRole(KEYCLOAK_ID, "LECTURER"))
                .assertNext(profile -> {
                    assertThat(profile.getMemberType()).isEqualTo("LECTURER");
                    assertThat(profile.getBorrowingLimit()).isEqualTo(10);
                    assertThat(profile.getLoanPeriodDays()).isEqualTo(30);
                    assertThat(profile.getReservationPriority()).isEqualTo(3);
                })
                .verifyComplete();

        verify(keycloakClient).removeRealmRole(KEYCLOAK_ID, "student");
        verify(keycloakClient).assignRealmRole(KEYCLOAK_ID, "lecturer");
        verify(keycloakClient).fetchRoleAttributes("lecturer");
    }

    @Test
    void fallsBackToExistingProfileValuesWhenKeycloakAttributeMissing() {
        // Only borrowingLimit is set on the role; loanPeriodDays and
        // reservationPriority must keep their existing profile values.
        when(keycloakClient.listUserRealmRoles(KEYCLOAK_ID)).thenReturn(Mono.just(List.of()));
        when(keycloakClient.assignRealmRole(KEYCLOAK_ID, "student")).thenReturn(Mono.empty());
        when(keycloakClient.fetchRoleAttributes("student"))
                .thenReturn(Mono.just(Map.of("borrowingLimit", List.of("7"))));

        MemberProfile existing = MemberProfile.builder()
                .id(KEYCLOAK_ID)
                .memberType("READER")
                .borrowingLimit(5)
                .loanPeriodDays(21)
                .reservationPriority(2)
                .createdAt(Instant.parse("2026-01-01T00:00:00Z"))
                .build();
        when(repository.findById(KEYCLOAK_ID)).thenReturn(Mono.just(existing));
        when(repository.save(any(MemberProfile.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(service.assignUserRole(KEYCLOAK_ID, "student"))
                .assertNext(profile -> {
                    assertThat(profile.getBorrowingLimit()).isEqualTo(7);          // overwritten
                    assertThat(profile.getLoanPeriodDays()).isEqualTo(21);         // kept
                    assertThat(profile.getReservationPriority()).isEqualTo(2);   // kept
                })
                .verifyComplete();
    }

    @Test
    void fallsBackToExistingProfileValuesWhenKeycloakAttributeIsNotAnInteger() {
        when(keycloakClient.listUserRealmRoles(KEYCLOAK_ID)).thenReturn(Mono.just(List.of()));
        when(keycloakClient.assignRealmRole(KEYCLOAK_ID, "student")).thenReturn(Mono.empty());
        when(keycloakClient.fetchRoleAttributes("student"))
                .thenReturn(Mono.just(Map.of("borrowingLimit", List.of("not-a-number"))));

        MemberProfile existing = MemberProfile.builder()
                .id(KEYCLOAK_ID)
                .memberType("READER")
                .borrowingLimit(9)
                .loanPeriodDays(14)
                .reservationPriority(0)
                .createdAt(Instant.parse("2026-01-01T00:00:00Z"))
                .build();
        when(repository.findById(KEYCLOAK_ID)).thenReturn(Mono.just(existing));
        when(repository.save(any(MemberProfile.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(service.assignUserRole(KEYCLOAK_ID, "student"))
                .assertNext(profile -> assertThat(profile.getBorrowingLimit()).isEqualTo(9)) // kept
                .verifyComplete();
    }

    @Test
    void proceedsAndKeepsProfileDefaultsWhenKeycloakAttributesFetchFails() {
        // Keycloak's GET /roles/{name} returning 5xx or 403 should NOT abort
        // the onboarding flow — the user still has the realm role assigned,
        // profile.memberType is still updated, and the three attribute columns
        // keep whatever the profile already had.
        when(keycloakClient.listUserRealmRoles(KEYCLOAK_ID)).thenReturn(Mono.just(List.of()));
        when(keycloakClient.assignRealmRole(KEYCLOAK_ID, "student")).thenReturn(Mono.empty());
        when(keycloakClient.fetchRoleAttributes("student"))
                .thenReturn(Mono.error(new IllegalStateException("KeycloakAdminClient downstream error")));

        MemberProfile existing = MemberProfile.builder()
                .id(KEYCLOAK_ID)
                .memberType("READER")
                .borrowingLimit(5)
                .loanPeriodDays(14)
                .reservationPriority(0)
                .createdAt(Instant.parse("2026-01-01T00:00:00Z"))
                .build();
        when(repository.findById(KEYCLOAK_ID)).thenReturn(Mono.just(existing));
        when(repository.save(any(MemberProfile.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(service.assignUserRole(KEYCLOAK_ID, "student"))
                .assertNext(profile -> {
                    assertThat(profile.getMemberType()).isEqualTo("STUDENT");
                    assertThat(profile.getBorrowingLimit()).isEqualTo(5);
                    assertThat(profile.getLoanPeriodDays()).isEqualTo(14);
                    assertThat(profile.getReservationPriority()).isEqualTo(0);
                })
                .verifyComplete();
    }

    @Test
    void acceptsCaseInsensitiveKeycloakAttributeKeys() {
        // Keycloak attribute keys are case-sensitive but operators occasionally
        // type them differently in the Admin Console. MemberProfileService
        // does a case-insensitive fallback so a typo doesn't silently wipe
        // the profile column.
        when(keycloakClient.listUserRealmRoles(KEYCLOAK_ID)).thenReturn(Mono.just(List.of()));
        when(keycloakClient.assignRealmRole(KEYCLOAK_ID, "student")).thenReturn(Mono.empty());
        when(keycloakClient.fetchRoleAttributes("student"))
                .thenReturn(Mono.just(Map.of(
                        "borrowinglimit", List.of("8"),     // all-lowercase
                        "LOANPERIODDAYS", List.of("28"),   // all-uppercase
                        "ReservationPriority", List.of("4"))));

        MemberProfile existing = MemberProfile.builder()
                .id(KEYCLOAK_ID)
                .memberType("READER")
                .borrowingLimit(5)
                .loanPeriodDays(14)
                .reservationPriority(0)
                .createdAt(Instant.parse("2026-01-01T00:00:00Z"))
                .build();
        when(repository.findById(KEYCLOAK_ID)).thenReturn(Mono.just(existing));
        when(repository.save(any(MemberProfile.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(service.assignUserRole(KEYCLOAK_ID, "student"))
                .assertNext(profile -> {
                    assertThat(profile.getBorrowingLimit()).isEqualTo(8);
                    assertThat(profile.getLoanPeriodDays()).isEqualTo(28);
                    assertThat(profile.getReservationPriority()).isEqualTo(4);
                })
                .verifyComplete();
    }

    @Test
    void rollsBackKeycloakRoleWhenProfileUpdateFails() {
        when(keycloakClient.listUserRealmRoles(KEYCLOAK_ID)).thenReturn(Mono.just(List.of()));
        when(keycloakClient.assignRealmRole(KEYCLOAK_ID, "student")).thenReturn(Mono.empty());
        when(keycloakClient.fetchRoleAttributes("student")).thenReturn(Mono.just(STUDENT_ATTRIBUTES));
        when(repository.findById(KEYCLOAK_ID)).thenReturn(Mono.empty()); // profile missing -> update fails
        when(keycloakClient.removeRealmRole(KEYCLOAK_ID, "student")).thenReturn(Mono.empty());

        StepVerifier.create(service.assignUserRole(KEYCLOAK_ID, "student"))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(ApiException.class);
                    ApiException apiException = (ApiException) error;
                    assertThat(apiException.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(apiException.getCode()).isEqualTo("MEMBER_PROFILE_NOT_FOUND");
                })
                .verify();

        verify(keycloakClient).assignRealmRole(KEYCLOAK_ID, "student");
        verify(keycloakClient).removeRealmRole(KEYCLOAK_ID, "student");
    }

    @Test
    void keepsOriginalErrorWhenRollbackAlsoFails() {
        when(keycloakClient.listUserRealmRoles(KEYCLOAK_ID)).thenReturn(Mono.just(List.of()));
        when(keycloakClient.assignRealmRole(KEYCLOAK_ID, "student")).thenReturn(Mono.empty());
        when(keycloakClient.fetchRoleAttributes("student")).thenReturn(Mono.just(STUDENT_ATTRIBUTES));
        when(repository.findById(KEYCLOAK_ID)).thenReturn(Mono.error(new IllegalStateException("database unavailable")));
        when(keycloakClient.removeRealmRole(KEYCLOAK_ID, "student"))
                .thenReturn(Mono.error(new IllegalStateException("Keycloak unavailable")));

        StepVerifier.create(service.assignUserRole(KEYCLOAK_ID, "student"))
                .expectError(IllegalStateException.class)
                .verify();

        verify(keycloakClient, times(1)).assignRealmRole(KEYCLOAK_ID, "student");
        verify(keycloakClient, times(1)).removeRealmRole(KEYCLOAK_ID, "student");
    }
}