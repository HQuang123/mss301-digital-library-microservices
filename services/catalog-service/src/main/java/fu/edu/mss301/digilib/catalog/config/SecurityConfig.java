package fu.edu.mss301.digilib.catalog.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Defense-in-depth JWT validation for catalog-service. The API gateway is the
 * primary enforcement point and already role-gates the mutating catalog
 * endpoints. This config makes catalog-service reject forged or
 * otherwise-unauthorized calls even if a caller bypasses the gateway (e.g. via
 * the internal Docker network) or if the gateway config drifts.
 *
 * <p>Access policy (mirrors the gateway):
 * <ul>
 *   <li>{@code GET /api/catalog/**} and {@code /files/**} — any authenticated
 *       user (students and lecturers can browse the catalog). Public preview
 *       paths that did <em>not</em> historically require auth (book covers,
 *       {@code /files/**}) are kept {@code permitAll}.</li>
 *   <li>{@code POST}, {@code PUT}, {@code PATCH}, {@code DELETE} on
 *       {@code /api/catalog/**} — {@code ADMIN} or {@code LIBRARIAN} realm
 *       role only.</li>
 *   <li>Actuator health/info are public; every other actuator endpoint is
 *       admin-only.</li>
 * </ul>
 */
@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/actuator/**").hasRole("ADMIN")
                        // /files/** serves book covers and is reachable through the gateway
                        // with permitAll — keep the same posture locally so seeded book
                        // covers render for unauthenticated browse traffic.
                        .requestMatchers(HttpMethod.GET, "/files/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/catalog/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/catalog/**")
                                .hasAnyRole("ADMIN", "LIBRARIAN")
                        .requestMatchers(HttpMethod.PUT, "/api/catalog/**")
                                .hasAnyRole("ADMIN", "LIBRARIAN")
                        .requestMatchers(HttpMethod.PATCH, "/api/catalog/**")
                                .hasAnyRole("ADMIN", "LIBRARIAN")
                        .requestMatchers(HttpMethod.DELETE, "/api/catalog/**")
                                .hasAnyRole("ADMIN", "LIBRARIAN")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtConverter())))
                .build();
    }

    /**
     * Reads Keycloak's {@code realm_access.roles} array and prefixes each entry
     * with {@code ROLE_} (uppercased), matching the converter used by the
     * gateway and the other business services.
     */
    private JwtAuthenticationConverter jwtConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthoritiesClaimName("realm_access.roles");
        authoritiesConverter.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            var realmAccess = jwt.getClaimAsMap("realm_access");
            if (realmAccess == null || !realmAccess.containsKey("roles")) {
                return java.util.Collections.<org.springframework.security.core.GrantedAuthority>emptyList();
            }
            @SuppressWarnings("unchecked")
            var roles = (java.util.List<String>) realmAccess.get("roles");
            java.util.List<org.springframework.security.core.GrantedAuthority> authorities =
                    new java.util.ArrayList<>();
            for (String r : roles) {
                authorities.add(new org.springframework.security.core.authority.SimpleGrantedAuthority(
                        "ROLE_" + r.toUpperCase()));
            }
            return authorities;
        });
        return converter;
    }
}