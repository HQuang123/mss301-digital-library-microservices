package fu.edu.mss301.digilib.catalog.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "book_copies")
@SQLRestriction("is_deleted = false")
public class BookCopy {

    public enum CopyStatus {
        DRAFT,
        AVAILABLE,
        BORROWED,
        RESERVED,
        LOST,
        DAMAGED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "copy_id")
    private Long copyId;

    @Column(name = "barcode", nullable = false, unique = true, length = 50)
    private String barcode;

    @Column(name = "shelf_location", length = 50, nullable = false)
    private String shelfLocation;

    @Column(name = "acquisition_date", nullable = false)
    private LocalDate acquisitionDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "copy_status", length = 30, nullable = false)
    @Builder.Default
    private CopyStatus copyStatus = CopyStatus.DRAFT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Builder.Default
    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    public void publish() {
        this.copyStatus = CopyStatus.AVAILABLE;
    }

    public void markAsBorrowed() {
        this.copyStatus = CopyStatus.BORROWED;
    }

    public void markAsLost() {
        this.copyStatus = CopyStatus.LOST;
    }

    public void markAsDamaged() {
        this.copyStatus = CopyStatus.DAMAGED;
    }

    public void softDelete() {
        this.isDeleted = true;
    }
}