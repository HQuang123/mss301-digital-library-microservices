package fu.edu.mss301.digilib.catalog.api.dto;

import fu.edu.mss301.digilib.catalog.domain.entity.Book;
import fu.edu.mss301.digilib.catalog.domain.entity.BookCopy;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class BookCopyResponse {
    private Long copyId;
    private String barcode;
    private String shelfLocation;
    private LocalDate acquisitionDate;
    private BookCopy.CopyStatus copyStatus;
    private Long bookId;

    public static BookCopyResponse from(BookCopy copy) {
        Book book = copy.getBook();
        return new BookCopyResponse(
                copy.getCopyId(),
                copy.getBarcode(),
                copy.getShelfLocation(),
                copy.getAcquisitionDate(),
                copy.getCopyStatus(),
                book != null ? book.getBookId() : null
        );
    }
}
