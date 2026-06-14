package fu.edu.mss301.digilib.catalog.infrastructure.adapter;

import fu.edu.mss301.digilib.catalog.domain.aggregate.BookAggregate;
import fu.edu.mss301.digilib.catalog.domain.entity.Book;
import fu.edu.mss301.digilib.catalog.domain.repository.BookRepository;
import fu.edu.mss301.digilib.catalog.infrastructure.persistence.BookAuditLogJpaRepository;
import fu.edu.mss301.digilib.catalog.infrastructure.persistence.BookCopyJpaRepository;
import fu.edu.mss301.digilib.catalog.infrastructure.persistence.BookJpaRepository;
import fu.edu.mss301.digilib.catalog.infrastructure.persistence.DigitalResourceJpaRepository;
import fu.edu.mss301.digilib.catalog.infrastructure.specification.BookSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class BookRepositoryAdapter implements BookRepository {

    private final BookJpaRepository bookJpaRepository;
    private final BookCopyJpaRepository bookCopyJpaRepository;
    private final DigitalResourceJpaRepository digitalResourceJpaRepository;
    private final BookAuditLogJpaRepository bookAuditLogJpaRepository;

    @Override
    public BookAggregate save(BookAggregate bookAggregate) {
        Book savedBook = bookJpaRepository.save(bookAggregate.getBook());

        bookAggregate.getBookCopies().forEach(copy -> copy.setBook(savedBook));
        bookAggregate.getDigitalResources().forEach(resource -> resource.setBook(savedBook));
        bookAggregate.getAuditLogs().forEach(log -> log.setBook(savedBook));

        deleteRemovedBookCopies(savedBook.getBookId(), bookAggregate);
        deleteRemovedDigitalResources(savedBook.getBookId(), bookAggregate);

        bookCopyJpaRepository.saveAll(bookAggregate.getBookCopies());
        digitalResourceJpaRepository.saveAll(bookAggregate.getDigitalResources());
        bookAuditLogJpaRepository.saveAll(bookAggregate.getAuditLogs());
        return bookAggregate;
    }

    @Override
    public Optional<BookAggregate> findAggregateByBookId(Long bookId) {
        return bookJpaRepository.findById(bookId)
                .map(book -> BookAggregate.rehydrate(
                        book,
                        bookCopyJpaRepository.findByBookBookId(bookId),
                        digitalResourceJpaRepository.findByBookBookId(bookId),
                        bookAuditLogJpaRepository.findByBookBookId(bookId)
                ));
    }

    @Override
    public void deleteBookById(Long bookId) {
        bookJpaRepository.deleteById(bookId);
    }

    @Override
    public Page<Book> findAllBooks(Pageable pageable) {
        return bookJpaRepository.findAll(pageable);
    }

    @Override
    public Optional<Book> findBookById(Long bookId) {
        return bookJpaRepository.findById(bookId);
    }

    @Override
    public Optional<Book> findBookByIsbn(String isbn) {
        return bookJpaRepository.findByIsbn(isbn);
    }

    @Override
    public Page<Book> searchBooks(String keyword, Pageable pageable) {
        return bookJpaRepository.findAll(BookSpecification.byKeyword(keyword), pageable);
    }

    @Override
    public Page<Book> findBooksByCategoryId(Long categoryId, Pageable pageable) {
        return bookJpaRepository.findAll(BookSpecification.byFilter(categoryId, null, null), pageable);
    }

    @Override
    public Page<Book> findBooksByClassificationId(Long classificationId, Pageable pageable) {
        return bookJpaRepository.findAll(BookSpecification.byFilter(null, classificationId, null), pageable);
    }

    @Override
    public Page<Book> findBooksByAvailabilityStatus(String availabilityStatus, Pageable pageable) {
        Book.BookStatus status = Book.BookStatus.valueOf(availabilityStatus.toUpperCase());
        return bookJpaRepository.findAll(BookSpecification.byFilter(null, null, status), pageable);
    }

    private void deleteRemovedBookCopies(Long bookId, BookAggregate bookAggregate) {
        Set<Long> retainedCopyIds = bookAggregate.getBookCopies().stream()
                .map(copy -> copy.getCopyId())
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        bookCopyJpaRepository.findByBookBookId(bookId).stream()
                .filter(copy -> !retainedCopyIds.contains(copy.getCopyId()))
                .forEach(bookCopyJpaRepository::delete);
    }

    private void deleteRemovedDigitalResources(Long bookId, BookAggregate bookAggregate) {
        Set<Long> retainedResourceIds = bookAggregate.getDigitalResources().stream()
                .map(resource -> resource.getResourceId())
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        digitalResourceJpaRepository.findByBookBookId(bookId).stream()
                .filter(resource -> !retainedResourceIds.contains(resource.getResourceId()))
                .forEach(digitalResourceJpaRepository::delete);
    }
}
