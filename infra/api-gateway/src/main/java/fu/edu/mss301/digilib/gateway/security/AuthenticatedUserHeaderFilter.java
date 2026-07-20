package fu.edu.mss301.digilib.gateway.security;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class AuthenticatedUserHeaderFilter implements GlobalFilter, Ordered {

    public static final String AUTHENTICATED_USER_ID_HEADER = "X-Authenticated-User-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return exchange.getPrincipal()
                .ofType(JwtAuthenticationToken.class)
                .map(authentication -> authentication.getToken().getSubject())
                .defaultIfEmpty("")
                .flatMap(userId -> {
                    var request = exchange.getRequest().mutate()
                            .headers(headers -> {
                                // Never trust an identity header supplied by the public caller.
                                headers.remove(AUTHENTICATED_USER_ID_HEADER);
                                if (!userId.isBlank()) {
                                    headers.set(AUTHENTICATED_USER_ID_HEADER, userId);
                                }
                            })
                            .build();
                    return chain.filter(exchange.mutate().request(request).build());
                });
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
