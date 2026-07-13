package fu.edu.mss301.digilib.loan.infrastructure.adapter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

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
        MemberResponse response = restClient.get()
                .uri("/api/v1/members/internal/{memberId}", memberId)
                .header("X-Internal-Api-Key", internalApiKey)
                .retrieve()
                .body(MemberResponse.class);

        if (response == null) {
            throw new IllegalStateException("Member service returned an empty response");
        }
        boolean hasDebt = response.outstandingBalance() != null
                && response.outstandingBalance().compareTo(BigDecimal.ZERO) > 0;
        if (hasDebt) {
            throw new IllegalStateException("Member has an outstanding balance");
        }
        return new MemberPolicy(response.borrowingLimit(), response.loanPeriodDays());
    }

    public record MemberPolicy(int borrowingLimit, int loanPeriodDays) {}

    private record MemberResponse(
            String id,
            int borrowingLimit,
            int loanPeriodDays,
            BigDecimal outstandingBalance
    ) {}
}
