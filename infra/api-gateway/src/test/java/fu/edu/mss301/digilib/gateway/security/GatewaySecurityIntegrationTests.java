package fu.edu.mss301.digilib.gateway.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.cors.reactive.CorsWebFilter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.cloud.config.enabled=false",
                "spring.cloud.discovery.enabled=false",
                "eureka.client.enabled=false",
                "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:8180/realms/digilib-realm",
                "gateway.cors.allowed-origins=http://localhost:5173"
        })
@AutoConfigureWebTestClient
class GatewaySecurityIntegrationTests {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private GatewayCorsProperties corsProperties;

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void bindsConfiguredCorsOrigins() {
        assertThat(corsProperties.getAllowedOrigins())
                .containsExactly("http://localhost:5173");
        assertThat(applicationContext.getBeansOfType(CorsWebFilter.class))
                .containsOnlyKeys("corsWebFilter");
    }

    @Test
    void rejectsProtectedRouteWithoutJwt() {
        webTestClient.get()
                .uri("/api/v1/members/me")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().contentTypeCompatibleWith("application/json")
                .expectHeader().valueEquals(HttpHeaders.WWW_AUTHENTICATE, "Bearer")
                .expectBody()
                .jsonPath("$.code").isEqualTo("AUTHENTICATION_REQUIRED")
                .jsonPath("$.path").isEqualTo("/api/v1/members/me")
                .jsonPath("$.requestId").isNotEmpty();
    }

    @Test
    void rejectsMalformedJwtWithTheSameStructuredError() {
        webTestClient.get()
                .uri("/api/v1/members/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().contentTypeCompatibleWith("application/json")
                .expectHeader().valueEquals(HttpHeaders.WWW_AUTHENTICATE, "Bearer")
                .expectBody()
                .jsonPath("$.code").isEqualTo("AUTHENTICATION_REQUIRED")
                .jsonPath("$.path").isEqualTo("/api/v1/members/me");
    }

    @Test
    void allowsPublicLoginRouteWithoutJwt() {
        webTestClient.post()
                .uri("/api/v1/auth/login")
                .exchange()
                .expectStatus().value(status ->
                        assertThat(status).isNotEqualTo(HttpStatus.UNAUTHORIZED.value()));
    }

    @Test
    void allowsProtectedRouteWithJwt() {
        webTestClient.mutateWith(mockJwt()
                        .jwt(jwt -> jwt.subject("member-1"))
                        .authorities(new SimpleGrantedAuthority("ROLE_MEMBER")))
                .get()
                .uri("/api/v1/members/me")
                .exchange()
                .expectStatus().value(status ->
                        assertThat(status).isNotIn(
                                HttpStatus.UNAUTHORIZED.value(),
                                HttpStatus.FORBIDDEN.value()));
    }

    @Test
    void rejectsBorrowRequestQueueForRegularMembers() {
        webTestClient.mutateWith(mockJwt()
                        .jwt(jwt -> jwt.subject("member-1"))
                        .authorities(new SimpleGrantedAuthority("ROLE_MEMBER")))
                .get()
                .uri("/api/v1/borrow-requests?status=PENDING")
                .exchange()
                .expectStatus().isForbidden()
                .expectBody()
                .jsonPath("$.code").isEqualTo("ACCESS_DENIED");
    }

    @Test
    void allowsBorrowRequestQueueForLibrarians() {
        webTestClient.mutateWith(mockJwt()
                        .jwt(jwt -> jwt.subject("librarian-1"))
                        .authorities(new SimpleGrantedAuthority("ROLE_LIBRARIAN")))
                .get()
                .uri("/api/v1/borrow-requests?status=PENDING")
                .exchange()
                .expectStatus().value(status ->
                        assertThat(status).isNotIn(
                                HttpStatus.UNAUTHORIZED.value(),
                                HttpStatus.FORBIDDEN.value()));
    }

    @Test
    void rejectsOperationalActuatorEndpointsForNonAdminUsers() {
        webTestClient.mutateWith(mockJwt()
                        .jwt(jwt -> jwt.subject("member-1"))
                        .authorities(new SimpleGrantedAuthority("ROLE_MEMBER")))
                .get()
                .uri("/actuator/metrics")
                .exchange()
                .expectStatus().isForbidden()
                .expectHeader().contentTypeCompatibleWith("application/json")
                .expectBody()
                .jsonPath("$.code").isEqualTo("ACCESS_DENIED")
                .jsonPath("$.path").isEqualTo("/actuator/metrics")
                .jsonPath("$.requestId").isNotEmpty();
    }

    @Test
    void allowsOperationalActuatorEndpointsForAdmins() {
        webTestClient.mutateWith(mockJwt()
                        .jwt(jwt -> jwt.subject("admin-1"))
                        .authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .get()
                .uri("/actuator/metrics")
                .exchange()
                .expectStatus().value(status ->
                        assertThat(status).isNotIn(
                                HttpStatus.UNAUTHORIZED.value(),
                                HttpStatus.FORBIDDEN.value()));
    }

    @Test
    void allowsCorsPreflightBeforeAuthentication() {
        webTestClient.options()
                .uri("http://localhost:8080/api/v1/members/me")
                .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpMethod.GET.name())
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
                        "http://localhost:5173");
    }

    @Test
    void rejectsCorsPreflightFromUnknownOrigin() {
        webTestClient.options()
                .uri("http://localhost:8080/api/v1/members/me")
                .header(HttpHeaders.ORIGIN, "https://untrusted.example")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpMethod.GET.name())
                .exchange()
                .expectStatus().isForbidden();
    }

}
