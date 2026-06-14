package fu.edu.mss301.digilib.catalog.domain.repository;

import fu.edu.mss301.digilib.catalog.domain.entity.BookCopy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface BookCopyRepository {

    BookCopy saveBookCopy(BookCopy bookCopy);

    Page<BookCopy> findAllBookCopies(Pageable pageable);

    Optional<BookCopy> findBookCopyById(Long copyId);

    Optional<BookCopy> findBookCopyByBarcode(String barcode);

    Page<BookCopy> findBookCopiesByBookId(Long bookId, Pageable pageable);

    Page<BookCopy> searchBookCopies(String keyword, Pageable pageable);

    Page<BookCopy> filterBookCopies(BookCopy.CopyStatus copyStatus, String shelfLocation, Pageable pageable);

    void deleteBookCopyById(Long copyId);
}
