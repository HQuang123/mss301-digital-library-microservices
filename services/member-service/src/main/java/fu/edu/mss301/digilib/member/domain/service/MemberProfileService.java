package fu.edu.mss301.digilib.member.domain.service;

import fu.edu.mss301.digilib.member.api.error.ApiException;
import fu.edu.mss301.digilib.member.domain.entity.MemberProfile;
import fu.edu.mss301.digilib.member.domain.repository.MemberProfileRepository;
import fu.edu.mss301.digilib.member.infrastructure.keycloak.KeycloakAdminClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberProfileService {

    /** The only role names a user may self-assign via the onboarding endpoint. */
    static final Set<String> ONBOARDING_ROLES = Set.of("student", "lecturer");

    private final MemberProfileRepository repository;
    private final KeycloakAdminClient keycloakClient;

    public Mono<MemberProfile> getProfileById(String id) {
        return repository.findById(id);
    }

    public Flux<MemberProfile> getAll() {
        return repository.findAll();
    }

    public Mono<MemberProfile> registerOrFetchProfile(String id, String email, String firstName, String lastName) {
        return repository.findById(id)
                .switchIfEmpty(Mono.defer(() -> {
                    Instant now = Instant.now();
                    // JIT Profile Generation logic for fresh accounts
                    MemberProfile newProfile = MemberProfile.builder()
                            .id(id)
                            .email(email)
                            .firstName(firstName != null ? firstName : "Library")
                            .lastName(lastName != null ? lastName : "Member")
                            .memberType("READER")
                            .memberCode("LIB-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                            .borrowingLimit(5)
                            .loanPeriodDays(14)
                            .outstandingBalance(BigDecimal.ZERO)
                            .status("UNLOCKED")
                            .createdAt(now)
                            .updatedAt(now)
                            .isNewRecord(true) // Crucial flag for R2DBC manual ID mapping
                            .build();
                    return repository.save(newProfile);
                }));
    }

    public Mono<MemberProfile> updateProfile(String id, String firstName, String lastName, String phone, String avatarKey) {
        return repository.findById(id)
                .flatMap(profile -> {
                    if (firstName != null) {
                        profile.setFirstName(firstName);
                    }
                    if (lastName != null) {
                        profile.setLastName(lastName);
                    }
                    if (phone != null) {
                        profile.setPhone(phone);
                    }
                    if (avatarKey != null) {
                        profile.setAvatarKey(avatarKey);
                    }
                    profile.setUpdatedAt(Instant.now());
                    return repository.save(profile);
                });
    }

    public Mono<MemberProfile> changeStatus(String id, String status) {
        return repository.findById(id)
                .flatMap(profile -> {
                    profile.setStatus(status.toUpperCase());
                    return repository.save(profile);
                });
    }

    /**
     * Self-service onboarding: assigns the chosen role ({@code student} or
     * {@code lecturer}) to the user in Keycloak, then mirrors the role into
     * the profile by writing both {@code memberType} and the role's
     * attributes (loanPeriodDays, borrowingLimit, reservationPriority) onto
     * the profile columns.
     *
     * <p>Re-calling this endpoint switches roles cleanly: any prior
     * student/lecturer assignment is removed before the new one is granted,
     * so the user always holds at most one onboarding role; the profile
     * attributes are then overwritten with the new role's attributes.
     *
     * <p>Saga order (at all times the user has at most one of
     * student/lecturer):
     * <ol>
     *   <li>Remove any existing student/lecturer roles from Keycloak.</li>
     *   <li>Assign the new role in Keycloak (succeeds → user holds the role).</li>
     *   <li>Fetch the role's attributes from Keycloak.</li>
     *   <li>Update {@code memberType} plus the three profile attribute columns
     *       from the role's attributes.  If a role attribute is missing or
     *       unparseable, fall back to the existing profile value so a
     *       misconfigured role can't wipe a useful default.</li>
     * </ol>
     * If step 4 fails, the new role assignment is rolled back (best-effort)
     * so the Keycloak and database states stay consistent.
     */
    public Mono<MemberProfile> assignUserRole(String keycloakSub, String role) {
        String normalized = role == null ? "" : role.trim().toLowerCase();
        if (!ONBOARDING_ROLES.contains(normalized)) {
            return Mono.error(new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_ROLE",
                    "Role must be 'student' or 'lecturer'."));
        }
        String memberType = normalized.toUpperCase();

        Mono<Map<String, List<String>>> attributes = keycloakClient.fetchRoleAttributes(normalized)
                .onErrorResume(error -> {
                    // If the role-rep attributes can't be fetched, log and proceed
                    // with an empty map — the profile update will fallback to the
                    // existing values rather than fail the whole onboarding.
                    log.warn("Could not load attributes for realm role '{}'; falling back to profile defaults: {}",
                            normalized, error.getMessage());
                    return Mono.just(Map.of());
                });

        return keycloakClient.listUserRealmRoles(keycloakSub)
                .flatMap(existingRoles -> {
                    Mono<Void> cleanup = Mono.empty();
                    for (String existing : existingRoles) {
                        String existingLower = existing.toLowerCase();
                        if (ONBOARDING_ROLES.contains(existingLower) && !existingLower.equals(normalized)) {
                            cleanup = cleanup.then(keycloakClient.removeRealmRole(keycloakSub, existing));
                        }
                    }
                    return cleanup;
                })
                .then(keycloakClient.assignRealmRole(keycloakSub, normalized))
                .then(attributes)
                .flatMap(attrs -> applyRoleToProfile(keycloakSub, memberType, attrs))
                .onErrorResume(error -> {
                    log.warn("Onboarding profile update failed for {}; rolling back Keycloak role '{}': {}",
                            keycloakSub, normalized, error.getMessage());
                    return keycloakClient.removeRealmRole(keycloakSub, normalized)
                            .onErrorResume(rollbackError -> {
                                log.error("Rollback failed for Keycloak user {} role '{}': {}",
                                        keycloakSub, normalized, rollbackError.getMessage());
                                return Mono.empty();
                            })
                            .then(Mono.error(error));
                });
    }

    private Mono<MemberProfile> applyRoleToProfile(String id,
                                                   String memberType,
                                                   Map<String, List<String>> attributes) {
        return repository.findById(id)
                .flatMap(profile -> {
                    profile.setMemberType(memberType);
                    profile.setBorrowingLimit(parseIntAttribute(attributes,
                            "borrowingLimit", profile.getBorrowingLimit()));
                    profile.setLoanPeriodDays(parseIntAttribute(attributes,
                            "loanPeriodDays", profile.getLoanPeriodDays()));
                    profile.setReservationPriority(parseIntAttribute(attributes,
                            "reservationPriority", profile.getReservationPriority()));
                    profile.setUpdatedAt(Instant.now());
                    return repository.save(profile);
                })
                .switchIfEmpty(Mono.error(new ApiException(
                        HttpStatus.NOT_FOUND,
                        "MEMBER_PROFILE_NOT_FOUND",
                        "Member profile not found. Please complete registration first.")));
    }

    /**
     * Reads the first value of {@code attributeName} from the Keycloak role
     * attributes map (Keycloak stores multi-valued attributes as
     * {@code List<String>}).  Returns {@code fallback} if the attribute is
     * missing or its value cannot be parsed as an integer — never throws.
     */
    private int parseIntAttribute(Map<String, List<String>> attributes,
                                 String attributeName,
                                 int fallback) {
        if (attributes == null || attributes.isEmpty()) {
            return fallback;
        }
        List<String> values = attributes.get(attributeName);
        if (values == null || values.isEmpty()) {
            // Keycloak attribute keys are case-sensitive but the convention
            // in this realm is camelCase; fall back to a case-insensitive scan
            // so a typo in Admin Console doesn't silently lose the value.
            for (Map.Entry<String, List<String>> entry : attributes.entrySet()) {
                if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(attributeName)
                        && entry.getValue() != null && !entry.getValue().isEmpty()) {
                    values = entry.getValue();
                    break;
                }
            }
            if (values == null || values.isEmpty()) {
                return fallback;
            }
        }
        try {
            return Integer.parseInt(values.get(0).trim());
        } catch (NumberFormatException e) {
            log.warn("Role attribute '{}' value '{}' is not an integer; using existing profile value {}",
                    attributeName, values.get(0), fallback);
            return fallback;
        }
    }
}
