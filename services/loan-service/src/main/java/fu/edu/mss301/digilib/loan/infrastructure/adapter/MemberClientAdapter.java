package fu.edu.mss301.digilib.loan.infrastructure.adapter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class MemberClientAdapter {
    private final RestClient restClient;
    private final String internalApiKey;

    public MemberClientAdapter(
            RestClient.Builder restClientBuilder,
            @Value("${services.member.base-url}") String memberServiceBaseUrl,
            @Value("${services.internal-api-key}") String internalApiKey
    ) {
        this.internalApiKey = internalApiKey;
        this.restClient = restClientBuilder
                .baseUrl(memberServiceBaseUrl)
                .build();
    }

    public MemberPolicy getPolicy(String memberId) {
        MemberDetails member = getMember(memberId);
        return new MemberPolicy(member.borrowingLimit(), member.loanPeriodDays());
    }

    public MemberDetails getMember(String memberId) {
        MemberResponse response = restClient.get()
                .uri("/api/v1/members/internal/{memberId}", memberId)
                .header("X-Internal-Api-Key", internalApiKey)
                .retrieve()
                .body(MemberResponse.class);

        if (response == null) {
            throw new IllegalStateException("Member service returned an empty response");
        }
        return new MemberDetails(
                response.id(), response.email(), response.memberCode(),
                response.borrowingLimit(), response.loanPeriodDays());
    }

    public record MemberPolicy(int borrowingLimit, int loanPeriodDays) {}

    public record MemberDetails(
            String id,
            String email,
            String memberCode,
            int borrowingLimit,
            int loanPeriodDays
    ) {}

    private record MemberResponse(
            String id,
            String email,
            String memberCode,
            int borrowingLimit,
            int loanPeriodDays
    ) {}
}
