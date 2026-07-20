package fu.edu.mss301.digilib.gateway.security;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class AuthenticatedUserHeaderFilterTests {

    private final AuthenticatedUserHeaderFilter filter = new AuthenticatedUserHeaderFilter();

    @Test
    void replacesCallerSuppliedIdentityWithJwtSubject() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("member-from-token")
                .build();
        JwtAuthenticationToken authentication = new JwtAuthenticationToken(jwt);
        ServerWebExchange exchange = MockServerWebExchange
                .from(MockServerHttpRequest.get("/api/v1/loans/my-loans")
                        .header(AuthenticatedUserHeaderFilter.AUTHENTICATED_USER_ID_HEADER, "spoofed-member")
                        .build())
                .mutate()
                .principal(Mono.just(authentication))
                .build();
        AtomicReference<String> forwardedUserId = new AtomicReference<>();

        filter.filter(exchange, capturingChain(forwardedUserId)).block();

        assertThat(forwardedUserId.get()).isEqualTo("member-from-token");
    }

    @Test
    void stripsCallerSuppliedIdentityWhenRequestIsAnonymous() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/public")
                        .header(AuthenticatedUserHeaderFilter.AUTHENTICATED_USER_ID_HEADER, "spoofed-member")
                        .build());
        AtomicReference<String> forwardedUserId = new AtomicReference<>();

        filter.filter(exchange, capturingChain(forwardedUserId)).block();

        assertThat(forwardedUserId.get()).isNull();
    }

    private GatewayFilterChain capturingChain(AtomicReference<String> forwardedUserId) {
        return exchange -> {
            forwardedUserId.set(exchange.getRequest().getHeaders().getFirst(
                    AuthenticatedUserHeaderFilter.AUTHENTICATED_USER_ID_HEADER));
            return Mono.empty();
        };
    }
}
