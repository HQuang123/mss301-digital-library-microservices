package fu.edu.mss301.digilib.gateway.resilience;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.cloud.gateway.route.RouteLocator;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.config.additional-location=optional:file:../config-repo/api-gateway.yml",
                "spring.cloud.config.enabled=false",
                "spring.cloud.discovery.enabled=false",
                "eureka.client.enabled=false",
                "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:8180/realms/digilib-realm"
        })
class GatewayResilienceConfigurationTests {

    private static final Set<String> RETRIED_ROUTES =
            Set.of("catalog-service", "loan-service", "member-service", "fine-service", "notification-service");

    @Autowired
    private RouteDefinitionLocator routeDefinitionLocator;

    @Autowired
    private RouteLocator routeLocator;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Autowired
    private TimeLimiterRegistry timeLimiterRegistry;

    @Autowired
    private BulkheadRegistry bulkheadRegistry;

    @Test
    void bindsEveryConfiguredRouteAndGatewayFilter() {
        // Resolving Route objects forces Spring Cloud Gateway to instantiate and
        // bind every named filter, catching invalid status/exception class names.
        List<String> routeIds = routeLocator.getRoutes()
                .map(route -> route.getId())
                .collectList()
                .block();

        assertThat(routeIds).contains(
                "catalog-service",
                "catalog-files",
                "loan-service",
                "member-auth",
                "member-service",
                "fine-service",
                "notification-service");
        assertThat(routeIds).doesNotContain("catalog-bulkhead-demo");
    }

    @Test
    void usesAnIndependentCircuitBreakerForEveryDownstreamRoute() {
        Map<String, RouteDefinition> routes = routeDefinitions();

        Set<String> breakerNames = routes.values().stream()
                .map(route -> filter(route, "CircuitBreaker"))
                .map(filter -> filter.getArgs().get("name"))
                .collect(Collectors.toSet());

        assertThat(routes).containsOnlyKeys(
                "catalog-service",
                "catalog-files",
                "loan-service",
                "member-auth",
                "member-service",
                "fine-service",
                "notification-service");
        assertThat(breakerNames).hasSameSizeAs(routes.values()).doesNotContainNull();
        assertThat(breakerNames).allSatisfy(name -> {
            assertThat(circuitBreakerRegistry.find(name)).isPresent();
            assertThat(timeLimiterRegistry.find(name)).isPresent();
            assertThat(bulkheadRegistry.find(name)).isPresent();
        });
    }

    @Test
    void retriesOnlyIdempotentGetRoutes() {
        Map<String, RouteDefinition> routes = routeDefinitions();

        routes.forEach((routeId, route) -> {
            List<FilterDefinition> retryFilters = route.getFilters().stream()
                    .filter(filter -> filter.getName().equals("Retry"))
                    .toList();

            if (RETRIED_ROUTES.contains(routeId)) {
                assertThat(retryFilters).singleElement().satisfies(filter -> {
                    assertThat(filter.getArgs().get("methods")).isEqualTo("GET");
                    assertThat(filter.getArgs().get("retries")).isEqualTo("2");
                });
            } else {
                assertThat(retryFilters).isEmpty();
            }
        });
    }

    private Map<String, RouteDefinition> routeDefinitions() {
        return routeDefinitionLocator.getRouteDefinitions()
                .collectMap(RouteDefinition::getId, Function.identity())
                .block();
    }

    private FilterDefinition filter(RouteDefinition route, String name) {
        return route.getFilters().stream()
                .filter(candidate -> candidate.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "Route " + route.getId() + " is missing filter " + name));
    }
}
