package fu.edu.mss301.digilib.gateway.security;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.stream.Collectors;

/**
 * Forwards the caller's identity as trusted headers so downstream business
 * services (e.g. fine-service) can enforce per-user authorization (which
 * studentId a caller may query) without each needing their own JWT
 * decoding/Spring Security setup — all JWT verification stays at the gateway.
 *
 * This is only safe because business services are reachable only through the
 * gateway (see docs/infra/security-bootstrap.md, "Direct-access boundary").
 * Inbound copies of these headers are always stripped first, so a caller
 * cannot spoof identity by setting them directly.
 */
@Component
public class UserContextHeadersFilter implements GlobalFilter, Ordered {

    static final String USER_ID_HEADER = "X-User-Id";
    static final String USER_ROLES_HEADER = "X-User-Roles";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return exchange.getPrincipal()
                .cast(Authentication.class)
                .filter(JwtAuthenticationToken.class::isInstance)
                .cast(JwtAuthenticationToken.class)
                .map(auth -> withUserHeaders(exchange, auth))
                .defaultIfEmpty(withoutUserHeaders(exchange))
                .flatMap(chain::filter);
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE - 1;
    }

    private ServerWebExchange withUserHeaders(ServerWebExchange exchange, JwtAuthenticationToken auth) {
        Jwt jwt = auth.getToken();
        String roles = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .headers(headers -> {
                    headers.remove(USER_ID_HEADER);
                    headers.remove(USER_ROLES_HEADER);
                    headers.set(USER_ID_HEADER, jwt.getSubject());
                    headers.set(USER_ROLES_HEADER, roles);
                })
                .build();

        return exchange.mutate().request(mutatedRequest).build();
    }

    private ServerWebExchange withoutUserHeaders(ServerWebExchange exchange) {
        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .headers(headers -> {
                    headers.remove(USER_ID_HEADER);
                    headers.remove(USER_ROLES_HEADER);
                })
                .build();

        return exchange.mutate().request(mutatedRequest).build();
    }
}
