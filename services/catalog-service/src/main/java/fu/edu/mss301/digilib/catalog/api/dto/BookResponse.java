package fu.edu.mss301.digilib.catalog.api.dto;

import fu.edu.mss301.digilib.catalog.domain.entity.Book;
import fu.edu.mss301.digilib.catalog.domain.entity.Category;
import fu.edu.mss301.digilib.catalog.domain.entity.Classification;
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
public class BookResponse {
    private Long bookId;
    private String isbn;
    private String title;
    private String author;
    private String publisher;
    private Integer publicationYear;
    private String edition;
    private String availabilityStatus;
    private String language;
    private String description;
    private String coverImageUrl;
    private Long categoryId;
    private String categoryName;
    private Long classificationId;
    private String classificationName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static BookResponse from(Book book) {
        Category category = book.getCategory();
        Classification classification = book.getClassification();
        return new BookResponse(
                book.getBookId(),
                book.getIsbn(),
                book.getTitle(),
                book.getAuthor(),
                book.getPublisher(),
                book.getPublicationYear(),
                book.getEdition(),
                book.getAvailabilityStatus(),
                book.getLanguage(),
                book.getDescription(),
                book.getCoverImageUrl(),
                category != null ? category.getCategoryId() : null,
                category != null ? category.getCategoryName() : null,
                classification != null ? classification.getClassificationId() : null,
                classification != null ? classification.getClassificationName() : null,
                book.getCreatedAt(),
                book.getUpdatedAt()
        );
    }
}
