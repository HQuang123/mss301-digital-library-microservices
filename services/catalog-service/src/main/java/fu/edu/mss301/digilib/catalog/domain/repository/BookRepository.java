package fu.edu.mss301.digilib.catalog.domain.repository;

import fu.edu.mss301.digilib.catalog.domain.aggregate.BookAggregate;
import fu.edu.mss301.digilib.catalog.domain.entity.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface BookRepository {

    BookAggregate save(BookAggregate bookAggregate);

    Optional<BookAggregate> findAggregateByBookId(Long bookId);

    void deleteBookById(Long bookId);

    Page<Book> findAllBooks(Pageable pageable);

    Page<Book> findDeletedBooks(Pageable pageable);

    Optional<Book> findBookById(Long bookId);

    Optional<Book> findBookByIdIncludingDeleted(Long bookId);

    Optional<Book> findBookByIsbn(String isbn);

    Page<Book> searchBooks(String keyword, Pageable pageable);

    Page<Book> findBooksByCategoryId(Long categoryId, Pageable pageable);

    Page<Book> findBooksByClassificationId(Long classificationId, Pageable pageable);

    Page<Book> findBooksByAvailabilityStatus(String availabilityStatus, Pageable pageable);

    Optional<BookAggregate> findAggregateByBookIdIncludingDeleted(Long bookId);
}
