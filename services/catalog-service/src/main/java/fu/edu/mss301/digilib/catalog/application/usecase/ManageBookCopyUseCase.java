package fu.edu.mss301.digilib.catalog.application.usecase;

import fu.edu.mss301.digilib.catalog.application.command.BookCopyCommand;
import fu.edu.mss301.digilib.catalog.domain.aggregate.BookAggregate;
import fu.edu.mss301.digilib.catalog.domain.entity.BookCopy;
import fu.edu.mss301.digilib.catalog.domain.repository.BookCopyRepository;
import fu.edu.mss301.digilib.catalog.domain.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ManageBookCopyUseCase {

    private final BookRepository bookRepository;
    private final BookCopyRepository bookCopyRepository;

    public BookAggregate add(BookCopyCommand command) {
        BookAggregate aggregate = findAggregate(command.getBookId());
        aggregate.addBookCopy(
                command.getBarcode(),
                command.getShelfLocation(),
                command.getAcquisitionDate(),
                command.getCopyStatus(),
                command.getUserId()
        );
        return bookRepository.save(aggregate);
    }

    public BookAggregate update(BookCopyCommand command) {
        BookAggregate aggregate = findAggregate(command.getBookId());
        aggregate.updateBookCopy(
                command.getCopyId(),
                command.getBarcode(),
                command.getShelfLocation(),
                command.getAcquisitionDate(),
                command.getCopyStatus(),
                command.getUserId()
        );
        return bookRepository.save(aggregate);
    }

    public BookAggregate delete(BookCopyCommand command) {
        BookAggregate aggregate = findAggregate(command.getBookId());
        aggregate.removeBookCopy(command.getCopyId(), command.getUserId());
        return bookRepository.save(aggregate);
    }

    public BookAggregate restore(BookCopyCommand command) {
        BookCopy deletedCopy = bookCopyRepository.findDeletedBookCopyById(command.getCopyId())
                .orElseThrow(() -> new IllegalArgumentException("Deleted book copy not found"));
        Long bookId = deletedCopy.getBook() != null ? deletedCopy.getBook().getBookId() : command.getBookId();
        if (bookId == null) {
            throw new IllegalArgumentException("Book copy is not assigned to a book");
        }

        bookCopyRepository.restoreBookCopyById(command.getCopyId());

        BookAggregate aggregate = findAggregate(bookId);
        aggregate.updateCopyStatus(command.getCopyId(), deletedCopy.getCopyStatus(), command.getUserId());
        return bookRepository.save(aggregate);
    }

    public BookAggregate updateStatus(BookCopyCommand command) {
        BookAggregate aggregate = findAggregate(command.getBookId());
        aggregate.updateCopyStatus(command.getCopyId(), command.getCopyStatus(), command.getUserId());
        return bookRepository.save(aggregate);
    }

    public BookAggregate updateShelfLocation(BookCopyCommand command) {
        BookAggregate aggregate = findAggregate(command.getBookId());
        aggregate.updateCopyShelfLocation(
                command.getCopyId(),
                command.getShelfLocation(),
                command.getUserId()
        );
        return bookRepository.save(aggregate);
    }

    @Transactional(readOnly = true)
    public Page<BookCopy> findAll(Pageable pageable) {
        return bookCopyRepository.findAllBookCopies(pageable);
    }

    @Transactional(readOnly = true)
    public Page<BookCopy> findDeleted(Pageable pageable) {
        return bookCopyRepository.findDeletedBookCopies(pageable);
    }

    @Transactional(readOnly = true)
    public BookCopy findById(Long copyId) {
        return bookCopyRepository.findBookCopyById(copyId)
                .orElseThrow(() -> new IllegalArgumentException("Book copy not found"));
    }

    @Transactional(readOnly = true)
    public Page<BookCopy> findByBook(Long bookId, Pageable pageable) {
        return bookCopyRepository.findBookCopiesByBookId(bookId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<BookCopy> search(BookCopyCommand command, Pageable pageable) {
        return bookCopyRepository.searchBookCopies(command.getKeyword(), pageable);
    }

    @Transactional(readOnly = true)
    public Page<BookCopy> filter(BookCopyCommand command, Pageable pageable) {
        return bookCopyRepository.filterBookCopies(command.getCopyStatus(), command.getShelfLocation(), pageable);
    }

    private BookAggregate findAggregate(Long bookId) {
        return bookRepository.findAggregateByBookId(bookId)
                .orElseThrow(() -> new IllegalArgumentException("Book aggregate not found"));
    }
}
