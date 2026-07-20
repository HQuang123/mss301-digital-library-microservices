package fu.edu.mss301.digilib.loan.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/**", "/api/v1/loans/internal/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/borrow-requests")
                            .hasAnyRole("ADMIN", "LIBRARIAN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/borrow-requests/*/approve",
                                "/api/v1/borrow-requests/*/reject", "/api/v1/rent-books",
                                "/api/v1/loans/return", "/api/v1/loans/*/lost")
                            .hasAnyRole("ADMIN", "LIBRARIAN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/loans")
                            .hasAnyRole("ADMIN", "LIBRARIAN")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtConverter())))
                .build();
    }

    JwtAuthenticationConverter jwtConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            var realmAccess = jwt.getClaimAsMap("realm_access");
            if (realmAccess == null || !realmAccess.containsKey("roles")) return java.util.List.of();
            @SuppressWarnings("unchecked")
            var roles = (java.util.List<String>) realmAccess.get("roles");
            return roles.stream()
                    .<org.springframework.security.core.GrantedAuthority>map(role ->
                            new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                    "ROLE_" + role.toUpperCase()))
                    .toList();
        });
        return converter;
    }
}
