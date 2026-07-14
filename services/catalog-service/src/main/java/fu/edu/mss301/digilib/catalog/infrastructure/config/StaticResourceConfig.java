package fu.edu.mss301.digilib.catalog.infrastructure.config;

import fu.edu.mss301.digilib.catalog.infrastructure.storage.UploadPathResolver;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {
    private static final Logger log = LoggerFactory.getLogger(StaticResourceConfig.class);
    private static final String[] DEV_ALLOWED_ORIGINS = {
            "http://localhost:5173",
            "http://127.0.0.1:5173",
            "http://localhost:4173",
            "http://127.0.0.1:4173"
    };

    private final Path uploadRoot;

    public StaticResourceConfig(@Value("${catalog.upload-dir:uploads/files}") String uploadDir) {
        this.uploadRoot = UploadPathResolver.resolve(uploadDir);
    }

    @PostConstruct
    void logStaticResourceLocation() {
        log.info("Serving static files from '{}'", uploadRoot.toAbsolutePath().normalize());
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/files/**")
                .addResourceLocations(getResourceLocation());
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/files/**")
                .allowedOrigins(DEV_ALLOWED_ORIGINS)
                .allowedMethods("GET", "OPTIONS")
                .allowedHeaders("*");
    }

    private String getResourceLocation() {
        String location = uploadRoot.toUri().toString();
        return location.endsWith("/") ? location : location + "/";
    }
}
