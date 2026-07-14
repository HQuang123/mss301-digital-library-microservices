package fu.edu.mss301.digilib.member.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class HttpClientConfig {

    /**
     * A shared WebClient instance used by the Keycloak infrastructure adapters.
     * No base URL is set here — callers supply full URIs via KeycloakProperties.
     */
    @Bean
    public WebClient webClient(WebClient.Builder builder) {
        return builder.build();
    }
}
