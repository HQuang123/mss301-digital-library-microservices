package fu.edu.mss301.digilib.catalog.application.usecase;

import fu.edu.mss301.digilib.catalog.application.command.BookCommand;
import fu.edu.mss301.digilib.catalog.domain.aggregate.BookAggregate;
import fu.edu.mss301.digilib.catalog.domain.entity.Book;
import fu.edu.mss301.digilib.catalog.domain.entity.Category;
import fu.edu.mss301.digilib.catalog.domain.entity.Classification;
import fu.edu.mss301.digilib.catalog.domain.repository.BookRepository;
import fu.edu.mss301.digilib.catalog.domain.repository.CategoryRepository;
import fu.edu.mss301.digilib.catalog.domain.repository.ClassificationRepository;
import fu.edu.mss301.digilib.catalog.domain.vo.BookContent;
import fu.edu.mss301.digilib.catalog.domain.vo.Isbn;
import fu.edu.mss301.digilib.catalog.domain.vo.PublicationInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ManageBookUseCase {

    private final BookRepository bookRepository;
    private final CategoryRepository categoryRepository;
    private final ClassificationRepository classificationRepository;

    public BookAggregate execute(BookCommand command) {
        return create(command);
    }

    public BookAggregate create(BookCommand command) {
        bookRepository.findBookByIsbn(command.getIsbn())
                .ifPresent(book -> {
                    throw new IllegalArgumentException("Can not duplicate ISBN " + command.getIsbn());
                });

        Category category = categoryRepository.findCategoryById(command.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));
        Classification classification = classificationRepository.findClassificationById(command.getClassificationId())
                .orElseThrow(() -> new IllegalArgumentException("Classification not found"));

        BookAggregate aggregate = BookAggregate.create(
                new Isbn(command.getIsbn()),
                command.getTitle(),
                command.getAuthor(),
                new PublicationInfo(
                        command.getPublisher(),
                        command.getPublicationYear(),
                        command.getEdition(),
                        command.getLanguage()
                ),
                new BookContent(command.getDescription(), command.getCoverImageUrl()),
                category,
                classification,
                command.getUserId()
        );

        return bookRepository.save(aggregate);
    }

    public BookAggregate update(BookCommand command) {
        BookAggregate aggregate = findAggregate(command.getBookId());
        Category category = categoryRepository.findCategoryById(command.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));
        Classification classification = classificationRepository.findClassificationById(command.getClassificationId())
                .orElseThrow(() -> new IllegalArgumentException("Classification not found"));

        aggregate.updateBookInformation(
                command.getTitle(),
                command.getAuthor(),
                new PublicationInfo(
                        command.getPublisher(),
                        command.getPublicationYear(),
                        command.getEdition(),
                        command.getLanguage()
                ),
                new BookContent(command.getDescription(), command.getCoverImageUrl()),
                category,
                classification,
                command.getUserId()
        );

        return bookRepository.save(aggregate);
    }

    public BookAggregate updateStatus(BookCommand command) {
        BookAggregate aggregate = findAggregate(command.getBookId());
        aggregate.updateBookAvailabilityStatus(command.getAvailabilityStatus(), command.getUserId());
        return bookRepository.save(aggregate);
    }

    public BookAggregate updateCoverImage(BookCommand command) {
        BookAggregate aggregate = findAggregate(command.getBookId());
        aggregate.updateCoverImage(command.getCoverImageUrl(), command.getUserId());
        return bookRepository.save(aggregate);
    }

    public BookAggregate delete(BookCommand command) {
        BookAggregate aggregate = findAggregate(command.getBookId());
        aggregate.deleteBook(command.getUserId());
        return bookRepository.save(aggregate);
    }

    public BookAggregate restore(BookCommand command) {
        BookAggregate aggregate = findAggregateIncludingDeleted(command.getBookId());
        aggregate.restoreBook(command.getUserId());
        return bookRepository.save(aggregate);
    }

    public BookAggregate assignCategory(BookCommand command) {
        BookAggregate aggregate = findAggregate(command.getBookId());
        Category category = categoryRepository.findCategoryById(command.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));

        aggregate.assignCategory(category, command.getUserId());
        return bookRepository.save(aggregate);
    }

    public BookAggregate assignClassification(BookCommand command) {
        BookAggregate aggregate = findAggregate(command.getBookId());
        Classification classification = classificationRepository.findClassificationById(command.getClassificationId())
                .orElseThrow(() -> new IllegalArgumentException("Classification not found"));

        aggregate.assignClassification(classification, command.getUserId());
        return bookRepository.save(aggregate);
    }

    @Transactional(readOnly = true)
    public Page<Book> findAll(Pageable pageable) {
        return bookRepository.findAllBooks(pageable);
    }

    @Transactional(readOnly = true)
    public Page<Book> findDeleted(Pageable pageable) {
        return bookRepository.findDeletedBooks(pageable);
    }

    @Transactional(readOnly = true)
    public Book findById(Long bookId) {
        return bookRepository.findBookById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("Book not found"));
    }

    @Transactional(readOnly = true)
    public Page<Book> search(BookCommand command, Pageable pageable) {
        return bookRepository.searchBooks(command.getKeyword(), pageable);
    }

    @Transactional(readOnly = true)
    public Page<Book> findByCategory(Long categoryId, Pageable pageable) {
        return bookRepository.findBooksByCategoryId(categoryId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Book> findByClassification(Long classificationId, Pageable pageable) {
        return bookRepository.findBooksByClassificationId(classificationId, pageable);
    }

    private BookAggregate findAggregate(Long bookId) {
        return bookRepository.findAggregateByBookId(bookId)
                .orElseThrow(() -> new IllegalArgumentException("Book aggregate not found"));
    }

    private BookAggregate findAggregateIncludingDeleted(Long bookId) {
        return bookRepository.findAggregateByBookIdIncludingDeleted(bookId)
                .orElseThrow(() -> new IllegalArgumentException("Book aggregate not found"));
    }
}
