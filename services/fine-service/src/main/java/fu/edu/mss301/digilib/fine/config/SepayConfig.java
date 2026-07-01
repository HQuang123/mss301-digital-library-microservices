package fu.edu.mss301.digilib.fine.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
@EnableConfigurationProperties(SepayProperties.class)
public class SepayConfig {

    @Bean
    Clock applicationClock() {
        return Clock.systemDefaultZone();
    }
}
