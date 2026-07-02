package fu.edu.mss301.digilib.member.domain.service;

import fu.edu.mss301.digilib.member.domain.entity.MemberProfile;
import fu.edu.mss301.digilib.member.domain.repository.MemberProfileRepository;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MemberProfileServiceTests {

    private final MemberProfileRepository repository = mock(MemberProfileRepository.class);
    private final MemberProfileService service = new MemberProfileService(repository);

    @Test
    void registerOrFetchProfileCreatesDefaultProfileWhenMissing() {
        when(repository.findById("member-1")).thenReturn(Mono.empty());
        when(repository.save(any(MemberProfile.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(service.registerOrFetchProfile("member-1", "reader@example.com", null, null))
                .assertNext(profile -> {
                    assertThat(profile.getId()).isEqualTo("member-1");
                    assertThat(profile.getEmail()).isEqualTo("reader@example.com");
                    assertThat(profile.getFirstName()).isEqualTo("Library");
                    assertThat(profile.getLastName()).isEqualTo("Member");
                    assertThat(profile.getMemberType()).isEqualTo("READER");
                    assertThat(profile.getBorrowingLimit()).isEqualTo(5);
                    assertThat(profile.getLoanPeriodDays()).isEqualTo(14);
                    assertThat(profile.getOutstandingBalance()).isEqualByComparingTo(BigDecimal.ZERO);
                    assertThat(profile.getStatus()).isEqualTo("UNLOCKED");
                    assertThat(profile.isNew()).isTrue();
                    assertThat(profile.getCreatedAt()).isNotNull();
                    assertThat(profile.getUpdatedAt()).isNotNull();
                })
                .verifyComplete();

        verify(repository).save(any(MemberProfile.class));
    }

    @Test
    void updateProfileOnlyChangesEditableProvidedFields() {
        MemberProfile existing = MemberProfile.builder()
                .id("member-1")
                .email("reader@example.com")
                .firstName("Old")
                .lastName("Name")
                .phone("123")
                .memberCode("LIB-00000001")
                .createdAt(Instant.parse("2026-01-01T00:00:00Z"))
                .updatedAt(Instant.parse("2026-01-01T00:00:00Z"))
                .build();

        when(repository.findById("member-1")).thenReturn(Mono.just(existing));
        when(repository.save(any(MemberProfile.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(service.updateProfile("member-1", "New", null, "456", null))
                .assertNext(profile -> {
                    assertThat(profile.getFirstName()).isEqualTo("New");
                    assertThat(profile.getLastName()).isEqualTo("Name");
                    assertThat(profile.getPhone()).isEqualTo("456");
                    assertThat(profile.getEmail()).isEqualTo("reader@example.com");
                    assertThat(profile.getMemberCode()).isEqualTo("LIB-00000001");
                    assertThat(profile.getUpdatedAt()).isAfter(Instant.parse("2026-01-01T00:00:00Z"));
                })
                .verifyComplete();
    }

    @Test
    void changeStatusUpdatesToUppercaseAndSaves() {
        MemberProfile existing = MemberProfile.builder()
                .id("member-1")
                .status("UNLOCKED")
                .build();

        when(repository.findById("member-1")).thenReturn(Mono.just(existing));
        when(repository.save(any(MemberProfile.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(service.changeStatus("member-1", "soft_locked"))
                .assertNext(profile -> {
                    assertThat(profile.getStatus()).isEqualTo("SOFT_LOCKED");
                })
                .verifyComplete();
    }
}
