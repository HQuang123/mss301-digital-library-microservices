package fu.edu.mss301.digilib.member;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "spring.flyway.enabled=false",
        "MEMBER_DB_USERNAME=test-only-database-user",
        "MEMBER_DB_PASSWORD=test-only-database-password",
        "keycloak.base-url=http://localhost:8180",
        "keycloak.realm=digilib-realm",
        "keycloak.issuer-uri=http://localhost:8180/realms/digilib-realm",
        "keycloak.client-id=digilib-auth",
        "keycloak.client-secret=test-only-client-secret",
        "services.internal-api-key=test-only-internal-api-key-with-more-than-32-characters",
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=${keycloak.issuer-uri}"
})
class MemberServiceApplicationTests {

    @Autowired
    private Environment environment;

    @Test
    void contextLoads() {
        assertThat(environment.getProperty("spring.security.oauth2.resourceserver.jwt.issuer-uri"))
                .isEqualTo("http://localhost:8180/realms/digilib-realm");
        assertThat(environment.getProperty("server.address")).isEqualTo("127.0.0.1");
    }

    @Test
    void postgresqlJdbcDriverIsAvailableForFlyway() throws ClassNotFoundException {
        assertThat(Class.forName("org.postgresql.Driver")).isNotNull();
    }

}
