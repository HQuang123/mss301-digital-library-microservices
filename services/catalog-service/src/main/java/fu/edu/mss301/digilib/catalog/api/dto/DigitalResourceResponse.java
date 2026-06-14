package fu.edu.mss301.digilib.catalog.api.dto;

import fu.edu.mss301.digilib.catalog.domain.entity.Book;
import fu.edu.mss301.digilib.catalog.domain.entity.DigitalResource;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class DigitalResourceResponse {
    private Long resourceId;
    private String fileFormat;
    private String resourceUrl;
    private String accessPermission;
    private LocalDateTime uploadedAt;
    private Long bookId;

    public static DigitalResourceResponse from(DigitalResource resource) {
        Book book = resource.getBook();
        return new DigitalResourceResponse(
                resource.getResourceId(),
                resource.getFileFormat(),
                resource.getResourceUrl(),
                resource.getAccessPermission(),
                resource.getUploadedAt(),
                book != null ? book.getBookId() : null
        );
    }
}
