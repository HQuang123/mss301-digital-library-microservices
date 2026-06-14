package fu.edu.mss301.digilib.catalog.domain.aggregate;

import fu.edu.mss301.digilib.catalog.domain.entity.BookAuditLog;
import fu.edu.mss301.digilib.catalog.domain.entity.DigitalResource;

import java.util.List;

final class DigitalResourceManager {

    private DigitalResourceManager() {
    }

    static void addDigitalResource(
            BookAggregate aggregate,
            String fileFormat,
            String resourceUrl,
            String accessPermission,
            Integer userId
    ) {
        validateFileFormat(fileFormat);
        validateResourceUrl(resourceUrl);
        validateAccessPermission(accessPermission);

        DigitalResource resource = DigitalResource.builder()
                .fileFormat(fileFormat)
                .resourceUrl(resourceUrl)
                .accessPermission(accessPermission)
                .book(aggregate.getBook())
                .build();

        aggregate.mutableDigitalResources().add(resource);
        addAuditLog(aggregate, userId);
    }

    static void updateDigitalResource(
            BookAggregate aggregate,
            Long resourceId,
            String fileFormat,
            String resourceUrl,
            String accessPermission,
            Integer userId
    ) {
        validateFileFormat(fileFormat);
        validateResourceUrl(resourceUrl);
        validateAccessPermission(accessPermission);

        DigitalResource resource = findDigitalResourceById(aggregate.mutableDigitalResources(), resourceId);
        resource.setFileFormat(fileFormat);
        resource.setResourceUrl(resourceUrl);
        resource.setAccessPermission(accessPermission);

        addAuditLog(aggregate, userId);
    }

    static void removeDigitalResource(BookAggregate aggregate, Long resourceId, Integer userId) {
        DigitalResource resource = findDigitalResourceById(aggregate.mutableDigitalResources(), resourceId);
        aggregate.mutableDigitalResources().remove(resource);
        addAuditLog(aggregate, userId);
    }

    static void updateDigitalResourceAccessPermission(
            BookAggregate aggregate,
            Long resourceId,
            String accessPermission,
            Integer userId
    ) {
        validateAccessPermission(accessPermission);

        DigitalResource resource = findDigitalResourceById(aggregate.mutableDigitalResources(), resourceId);
        resource.setAccessPermission(accessPermission);

        addAuditLog(aggregate, userId);
    }

    static DigitalResource accessDigitalResource(
            BookAggregate aggregate,
            Long resourceId,
            String requesterPermission
    ) {
        DigitalResource resource = findDigitalResourceById(aggregate.mutableDigitalResources(), resourceId);
        String requiredPermission = resource.getAccessPermission();

        if (requiredPermission != null
                && !"PUBLIC".equalsIgnoreCase(requiredPermission)
                && (requesterPermission == null || !requiredPermission.equalsIgnoreCase(requesterPermission))) {
            throw new IllegalArgumentException("Access denied for digital resource");
        }

        return resource;
    }

    private static DigitalResource findDigitalResourceById(List<DigitalResource> resources, Long resourceId) {
        return resources.stream()
                .filter(resource -> resource.getResourceId() != null && resource.getResourceId().equals(resourceId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Digital resource not found"));
    }

    private static void addAuditLog(BookAggregate aggregate, Integer userId) {
        if (userId != null) {
            aggregate.addAuditLog(BookAuditLog.AuditAction.UPDATE, userId);
        }
    }

    private static void validateFileFormat(String fileFormat) {
        if (fileFormat == null || fileFormat.trim().isEmpty()) {
            throw new IllegalArgumentException("File format cannot be empty");
        }
    }

    private static void validateResourceUrl(String resourceUrl) {
        if (resourceUrl == null || resourceUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("Resource URL cannot be empty");
        }
    }

    private static void validateAccessPermission(String accessPermission) {
        if (accessPermission == null || accessPermission.trim().isEmpty()) {
            throw new IllegalArgumentException("Access permission cannot be empty");
        }
    }
}
