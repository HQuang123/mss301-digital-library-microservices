package fu.edu.mss301.digilib.catalog.infrastructure.adapter;

import fu.edu.mss301.digilib.catalog.domain.entity.Book;
import fu.edu.mss301.digilib.catalog.domain.entity.BookCopy;
import fu.edu.mss301.digilib.catalog.domain.repository.InventoryRepository;
import fu.edu.mss301.digilib.catalog.infrastructure.persistence.BookCopyJpaRepository;
import fu.edu.mss301.digilib.catalog.infrastructure.persistence.BookJpaRepository;
import fu.edu.mss301.digilib.catalog.infrastructure.persistence.DigitalResourceJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class InventoryRepositoryAdapter implements InventoryRepository {

    private final BookJpaRepository bookJpaRepository;
    private final BookCopyJpaRepository bookCopyJpaRepository;
    private final DigitalResourceJpaRepository digitalResourceJpaRepository;

    @Override
    public InventoryOverview getInventoryOverview() {
        return new InventoryOverview(
                bookJpaRepository.count(),
                bookCopyJpaRepository.count(),
                bookCopyJpaRepository.countByCopyStatus(BookCopy.CopyStatus.AVAILABLE),
                bookCopyJpaRepository.countByCopyStatus(BookCopy.CopyStatus.BORROWED),
                bookCopyJpaRepository.countByCopyStatus(BookCopy.CopyStatus.RESERVED),
                bookCopyJpaRepository.countByCopyStatus(BookCopy.CopyStatus.LOST),
                bookCopyJpaRepository.countByCopyStatus(BookCopy.CopyStatus.DAMAGED),
                digitalResourceJpaRepository.count()
        );
    }

    @Override
    public List<BookCopyCount> countBookCopiesByBook() {
        return bookJpaRepository.findAll().stream()
                .map(this::toBookCopyCount)
                .toList();
    }

    @Override
    public InventoryStatus checkBookInventoryStatus(Long bookId) {
        Book book = bookJpaRepository.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("Book not found"));
        List<BookCopy> copies = bookCopyJpaRepository.findByBookBookId(bookId);

        return new InventoryStatus(
                book.getBookId(),
                book.getTitle(),
                book.getAvailabilityStatus(),
                copies.size(),
                countAvailableCopies(copies)
        );
    }

    @Override
    public InventoryReport getInventoryReport() {
        return new InventoryReport(
                getInventoryOverview(),
                countBookCopiesByBook(),
                bookJpaRepository.findAll().stream()
                        .map(book -> checkBookInventoryStatus(book.getBookId()))
                        .toList()
        );
    }

    private BookCopyCount toBookCopyCount(Book book) {
        List<BookCopy> copies = bookCopyJpaRepository.findByBookBookId(book.getBookId());
        return new BookCopyCount(
                book.getBookId(),
                book.getTitle(),
                copies.size(),
                countAvailableCopies(copies)
        );
    }

    private long countAvailableCopies(List<BookCopy> copies) {
        return copies.stream()
                .filter(copy -> copy.getCopyStatus() == BookCopy.CopyStatus.AVAILABLE)
                .count();
    }
}
