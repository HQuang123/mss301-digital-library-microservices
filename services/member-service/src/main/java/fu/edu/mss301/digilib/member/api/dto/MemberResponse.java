package fu.edu.mss301.digilib.member.api.dto;

import fu.edu.mss301.digilib.member.domain.entity.MemberProfile;

import java.math.BigDecimal;
import java.time.Instant;

public record MemberResponse(
        String id,
        String email,
        String firstName,
        String lastName,
        String phone,
        String memberType,
        String memberCode,
        int borrowingLimit,
        int loanPeriodDays,
        BigDecimal outstandingBalance,
        String avatarKey,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
    public static MemberResponse from(MemberProfile profile) {
        return new MemberResponse(
                profile.getId(),
                profile.getEmail(),
                profile.getFirstName(),
                profile.getLastName(),
                profile.getPhone(),
                profile.getMemberType(),
                profile.getMemberCode(),
                profile.getBorrowingLimit(),
                profile.getLoanPeriodDays(),
                profile.getOutstandingBalance(),
                profile.getAvatarKey(),
                profile.getStatus(),
                profile.getCreatedAt(),
                profile.getUpdatedAt()
        );
    }
}
