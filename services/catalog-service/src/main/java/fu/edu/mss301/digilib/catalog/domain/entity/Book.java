package fu.edu.mss301.digilib.catalog.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "books")
@Builder
@SQLRestriction("is_deleted = false")
public class Book {

    public enum BookStatus {
        DRAFT,
        ACTIVE,
        INACTIVE,
        ARCHIVED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "book_id")
    private Long bookId;

    @Column(name = "isbn", unique = true, length = 20)
    private String isbn;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "author", length = 150, nullable = false)
    private String author;

    @Column(name = "publisher", length = 150, nullable = false)
    private String publisher;

    @Column(name = "publication_year", nullable = false)
    private Integer publicationYear;

    @Column(name = "edition", length = 50, nullable = false)
    private String edition;

    @Enumerated(EnumType.STRING)
    @Column(name = "book_status", length = 20, nullable = false)
    @Builder.Default
    private BookStatus bookStatus = BookStatus.DRAFT;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "language", length = 50, nullable = false)
    private String language;

    @Column(name = "description", length = 500, nullable = false)
    private String description;

    @Column(name = "cover_image_url", length = 255)
    private String coverImageUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classification_id", nullable = false)
    private Classification classification;

    @Builder.Default
    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    public String getAvailabilityStatus() {
        return bookStatus != null ? bookStatus.name() : null;
    }

}
