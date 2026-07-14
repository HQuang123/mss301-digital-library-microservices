package fu.edu.mss301.digilib.catalog.infrastructure.persistence;

import java.time.LocalDateTime;

public interface DigitalResourceRowProjection {

    Long getResourceId();

    String getFileFormat();

    String getResourceUrl();

    String getAccessPermission();

    LocalDateTime getUploadedAt();

    Long getBookId();

    Boolean getIsDeleted();
}
