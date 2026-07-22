package fu.edu.mss301.digilib.gateway.security;

import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Blocks authenticated-but-not-yet-onboarded users from reaching any
 * downstream service until they have selected a role ({@code student} or
 * {@code lecturer}) via {@code PATCH /api/v1/members/me/role}.
 *
 * <p>A user is considered onboarded when their JWT carries any of the realm
 * roles {@code student}, {@code lecturer}, {@code librarian}, or
 * {@code admin}. {@code librarian} and {@code admin} are admin-granted in the
 * Keycloak console; {@code student}/{@code lecturer} are self-assigned via the
 * onboarding endpoint.
 *
 * <p>Whitelisted paths (always pass regardless of onboarding state):
 * <ul>
 *   <li>{@code /actuator/**}</li>
 *   <li>{@code /api/v1/auth/**}</li>
 *   <li>{@code /api/v1/members/me} — read/update own profile</li>
 *   <li>{@code /api/v1/members/me/role} — the onboarding call itself</li>
 * </ul>
 *
 * <p>Implemented as a {@link WebFilter} rather than a Spring Cloud Gateway
 * {@code GlobalFilter} because {@code GlobalFilter}s only run for matched
 * routes — unmatched paths would otherwise bypass the onboarding check and
 * return 404 instead of being rejected here. A {@link WebFilter} at order
 * {@code 0} runs after Spring Security's authorization WebFilter (negative
 * orders) but before the WebHandler chain performs routing.
 */
@Component
public class OnboardingRequiredFilter implements WebFilter, Ordered {

    private static final PathPatternParser PARSER = new PathPatternParser();
    private static final List<PathPattern> WHITELIST = List.of(
            PARSER.parse("/actuator/**"),
            PARSER.parse("/api/v1/auth/**"),
            PARSER.parse("/api/v1/members/me"),
            PARSER.parse("/api/v1/members/me/role")
    );

    /** Any one of these realm roles means the user is past onboarding. */
    private static final Set<String> ONBOARDED_ROLES = Set.of(
            "student", "lecturer", "librarian", "admin"
    );

    private final GatewaySecurityErrorWriter errorWriter;

    public OnboardingRequiredFilter(GatewaySecurityErrorWriter errorWriter) {
        this.errorWriter = errorWriter;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        var path = exchange.getRequest().getPath().pathWithinApplication();

        for (PathPattern pattern : WHITELIST) {
            if (pattern.matches(path)) {
                return chain.filter(exchange);
            }
        }

        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .filter(OnboardingRequiredFilter::hasJwtPrincipal)
                .flatMap(auth -> {
                    for (GrantedAuthority authority : auth.getAuthorities()) {
                        String name = authority.getAuthority();
                        if (name.startsWith("ROLE_")) {
                            String role = name.substring(5).toLowerCase(Locale.ROOT);
                            if (ONBOARDED_ROLES.contains(role)) {
                                return chain.filter(exchange);
                            }
                        }
                    }
                    return errorWriter.write(exchange, HttpStatus.FORBIDDEN,
                            "ONBOARDING_REQUIRED",
                            "Please complete account onboarding by selecting student or lecturer role.");
                })
                .switchIfEmpty(Mono.defer(() -> chain.filter(exchange)));
    }

    private static boolean hasJwtPrincipal(Authentication auth) {
        return auth != null && auth.isAuthenticated()
                && auth.getPrincipal() instanceof Jwt;
    }

    @Override
    public int getOrder() {
        // Spring Security's authorization WebFilter runs at a negative order
        // (SecurityWebFiltersOrder.AUTHORIZATION). 0 places us after every
        // security filter but before the WebHandler/route is dispatched.
        return 0;
    }
}