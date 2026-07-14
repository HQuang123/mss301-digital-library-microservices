package fu.edu.mss301.digilib.member.domain.service;

import fu.edu.mss301.digilib.member.domain.entity.MemberProfile;
import fu.edu.mss301.digilib.member.domain.repository.MemberProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MemberProfileService {

    private final MemberProfileRepository repository;

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
}
