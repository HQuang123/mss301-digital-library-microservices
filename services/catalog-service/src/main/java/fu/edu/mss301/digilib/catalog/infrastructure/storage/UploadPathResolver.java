package fu.edu.mss301.digilib.catalog.infrastructure.storage;

import java.nio.file.Path;
import java.nio.file.Paths;

public final class UploadPathResolver {
    private static final String MODULE_NAME = "catalog-service";
    private static final String SERVICES_DIR = "services";

    private UploadPathResolver() {
    }

    public static Path resolve(String uploadDir) {
        Path configuredPath = Paths.get(uploadDir);
        if (configuredPath.isAbsolute()) {
            return configuredPath.normalize();
        }

        Path moduleRoot = resolveCatalogServiceRoot();
        return moduleRoot.resolve(configuredPath).normalize();
    }

    public static Path resolveCatalogServiceRoot() {
        Path current = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();

        while (current != null) {
            if (isCatalogServiceModule(current)) {
                return current;
            }

            Path nestedModule = current.resolve(SERVICES_DIR).resolve(MODULE_NAME);
            if (isCatalogServiceModule(nestedModule)) {
                return nestedModule.toAbsolutePath().normalize();
            }

            current = current.getParent();
        }

        return Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
    }

    private static boolean isCatalogServiceModule(Path candidate) {
        if (candidate == null) {
            return false;
        }

        Path normalizedCandidate = candidate.toAbsolutePath().normalize();
        Path pomFile = normalizedCandidate.resolve("pom.xml");
        return MODULE_NAME.equals(normalizedCandidate.getFileName() != null ? normalizedCandidate.getFileName().toString() : null)
                && java.nio.file.Files.exists(pomFile);
    }
}
