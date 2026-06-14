package fu.edu.mss301.digilib.catalog.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class BookUpdateRequest {
    private String title;
    private String author;
    private String publisher;
    private Integer publicationYear;
    private String edition;
    private String language;
    private String description;
    private String coverImageUrl;
    private Long categoryId;
    private Long classificationId;
    private Integer userId;
}
