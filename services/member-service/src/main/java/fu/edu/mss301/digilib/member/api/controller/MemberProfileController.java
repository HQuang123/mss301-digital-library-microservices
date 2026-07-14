package fu.edu.mss301.digilib.member.api.controller;

import fu.edu.mss301.digilib.member.api.dto.MemberResponse;
import fu.edu.mss301.digilib.member.api.dto.MemberStatusRequest;
import fu.edu.mss301.digilib.member.api.dto.MemberUpdateRequest;
import fu.edu.mss301.digilib.member.domain.service.MemberProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/members")
public class MemberProfileController {

    private final MemberProfileService profileService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public Flux<MemberResponse> getAllMember() {
        return profileService.getAll()
                .switchIfEmpty(Flux.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Member list is empty")))
                .map(MemberResponse::from);
    }

    @PutMapping("/{memberId}/status")
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
    public Mono<MemberResponse> setMemberStatus(@PathVariable String memberId,
                                                @Valid @RequestBody MemberStatusRequest request) {
        return profileService.changeStatus(
                        memberId,
                        request.status()
                )
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Member profile not found")))
                .map(MemberResponse::from);
    }

    @Value("${services.internal-api-key}")
    private String internalApiKey;

    /**
     * Retrieves or creates a profile dynamically based on the validated identity context.
     */
    @GetMapping("/me")
    public Mono<MemberResponse> getMyProfile(@AuthenticationPrincipal Jwt jwt) {
        String keycloakSub = jwt.getSubject(); // Keycloak's immutable User UUID
        String email = jwt.getClaimAsString("email");
        String firstName = jwt.getClaimAsString("given_name");
        String lastName = jwt.getClaimAsString("family_name");

        return profileService.registerOrFetchProfile(keycloakSub, email, firstName, lastName)
                .map(MemberResponse::from);
    }

    @PatchMapping("/me")
    public Mono<MemberResponse> updateMyProfile(@AuthenticationPrincipal Jwt jwt,
                                                @Valid @RequestBody MemberUpdateRequest request) {
        return profileService.updateProfile(
                        jwt.getSubject(),
                        request.firstName(),
                        request.lastName(),
                        request.phone(),
                        request.avatarKey()
                )
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Member profile not found")))
                .map(MemberResponse::from);
    }

    /**
     * Endpoint for internal inter-service communication (e.g., Loan Service checking borrowing capability)
     */
    @GetMapping("/{memberId}")
    public Mono<MemberResponse> getProfileById(@PathVariable String memberId) {
        return profileService.getProfileById(memberId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Member profile not found")))
                .map(MemberResponse::from);
    }

    /**
     * Service-to-service endpoint. It intentionally does not accept end-user JWTs;
     * callers must provide the shared internal API key.
     */
    @GetMapping("/internal/{memberId}")
    public Mono<MemberResponse> getProfileForInternalService(
            @PathVariable("memberId") String memberId,
            @RequestHeader(name = "X-Internal-Api-Key") String suppliedApiKey) {
        if (!constantTimeEquals(internalApiKey, suppliedApiKey)) {
            return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid internal API key"));
        }
        return profileService.getProfileById(memberId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Member profile not found")))
                .map(MemberResponse::from);
    }

    private boolean constantTimeEquals(String expected, String supplied) {
        return expected != null && supplied != null && MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                supplied.getBytes(StandardCharsets.UTF_8));
    }
}
