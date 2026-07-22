package fu.edu.mss301.digilib.catalog;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(properties = {
		"spring.cloud.config.enabled=false",
		"spring.cloud.discovery.enabled=false",
		"eureka.client.enabled=false",
		"spring.jpa.hibernate.ddl-auto=validate",
		"spring.jpa.show-sql=false",
		// Required so the OAuth2 resource-server auto-config can build a JwtDecoder;
		// without it the security starter fails the context boot.
		"spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:8180/realms/digilib-realm"
})
class DigilibCatalogServiceApplicationTests {

	@Container
	@ServiceConnection
	static final PostgreSQLContainer<?> postgres =
			new PostgreSQLContainer<>("postgres:16-alpine");

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void contextLoads() {
	}

	@Test
	void appliesFlywayMigrationsAndSeedsCatalogData() {
		Integer books = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM books", Integer.class);
		Integer categories = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM categories", Integer.class);

		assertThat(books).isGreaterThan(0);
		assertThat(categories).isGreaterThan(0);
	}

}
