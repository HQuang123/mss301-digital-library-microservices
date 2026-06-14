package fu.edu.mss301.digilib.catalog.domain.aggregate;

import fu.edu.mss301.digilib.catalog.domain.entity.Book;
import fu.edu.mss301.digilib.catalog.domain.entity.BookAuditLog;
import fu.edu.mss301.digilib.catalog.domain.entity.BookCopy;
import fu.edu.mss301.digilib.catalog.domain.entity.Category;
import fu.edu.mss301.digilib.catalog.domain.entity.Classification;
import fu.edu.mss301.digilib.catalog.domain.entity.DigitalResource;
import fu.edu.mss301.digilib.catalog.domain.vo.BookContent;
import fu.edu.mss301.digilib.catalog.domain.vo.Isbn;
import fu.edu.mss301.digilib.catalog.domain.vo.PublicationInfo;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class BookAggregate {

    private Book book;

    private Isbn isbn;

    private PublicationInfo publicationInfo;

    private BookContent bookContent;

    private final List<BookCopy> bookCopies = new ArrayList<>();

    private final List<DigitalResource> digitalResources = new ArrayList<>();

    private final List<BookAuditLog> auditLogs = new ArrayList<>();

    public static BookAggregate create(
            Isbn isbn,
            String title,
            String author,
            PublicationInfo publicationInfo,
            BookContent bookContent,
            Category category,
            Classification classification,
            Integer userId
    ) {
        validateTitle(title);
        validateAuthor(author);
        validateBookDetails(isbn, publicationInfo, bookContent, category, classification);

        BookAggregate aggregate = new BookAggregate();
        aggregate.isbn = isbn;
        aggregate.publicationInfo = publicationInfo;
        aggregate.bookContent = bookContent;

        aggregate.book = Book.builder()
                .isbn(isbn.getValue())
                .title(title)
                .author(author)
                .publisher(publicationInfo.getPublisher())
                .publicationYear(publicationInfo.getPublicationYear())
                .edition(publicationInfo.getEdition())
                .language(publicationInfo.getLanguage())
                .description(bookContent.getDescription())
                .coverImageUrl(bookContent.getCoverImageUrl())
                .bookStatus(Book.BookStatus.ACTIVE)
                .category(category)
                .classification(classification)
                .build();

        aggregate.addAuditLog(BookAuditLog.AuditAction.CREATE, userId);
        return aggregate;
    }

    public static BookAggregate rehydrate(
            Book book,
            List<BookCopy> bookCopies,
            List<DigitalResource> digitalResources,
            List<BookAuditLog> auditLogs
    ) {
        if (book == null) {
            throw new IllegalArgumentException("Book cannot be null");
        }

        BookAggregate aggregate = new BookAggregate();
        aggregate.book = book;
        aggregate.isbn = new Isbn(book.getIsbn());
        aggregate.publicationInfo = new PublicationInfo(
                book.getPublisher(),
                book.getPublicationYear(),
                book.getEdition(),
                book.getLanguage()
        );
        aggregate.bookContent = new BookContent(book.getDescription(), book.getCoverImageUrl());
        aggregate.bookCopies.addAll(bookCopies);
        aggregate.digitalResources.addAll(digitalResources);
        aggregate.auditLogs.addAll(auditLogs);
        return aggregate;
    }

    public void updateBookInformation(
            String title,
            String author,
            PublicationInfo publicationInfo,
            BookContent bookContent,
            Category category,
            Classification classification,
            Integer userId
    ) {
        validateTitle(title);
        validateAuthor(author);
        validateBookDetails(isbn, publicationInfo, bookContent, category, classification);

        this.publicationInfo = publicationInfo;
        this.bookContent = bookContent;

        book.setTitle(title);
        book.setAuthor(author);
        book.setPublisher(publicationInfo.getPublisher());
        book.setPublicationYear(publicationInfo.getPublicationYear());
        book.setEdition(publicationInfo.getEdition());
        book.setLanguage(publicationInfo.getLanguage());
        book.setDescription(bookContent.getDescription());
        book.setCoverImageUrl(bookContent.getCoverImageUrl());
        book.setCategory(category);
        book.setClassification(classification);

        addAuditLog(BookAuditLog.AuditAction.UPDATE, userId);
    }

    public void assignCategory(Category category, Integer userId) {
        if (category == null) {
            throw new IllegalArgumentException("Category cannot be null");
        }

        book.setCategory(category);
        addAuditLog(BookAuditLog.AuditAction.UPDATE, userId);
    }

    public void assignClassification(Classification classification, Integer userId) {
        if (classification == null) {
            throw new IllegalArgumentException("Classification cannot be null");
        }

        book.setClassification(classification);
        addAuditLog(BookAuditLog.AuditAction.UPDATE, userId);
    }

    public void updateBookAvailabilityStatus(String availabilityStatus, Integer userId) {
        Book.BookStatus bookStatus = parseBookStatus(availabilityStatus);
        book.setBookStatus(bookStatus);
        addAuditLog(BookAuditLog.AuditAction.UPDATE, userId);
    }

    public void deleteBook(Integer userId) {
        book.setBookStatus(Book.BookStatus.ARCHIVED);
        book.setIsDeleted(true);
        addAuditLog(BookAuditLog.AuditAction.DELETE, userId);
    }

    public void addBookCopy(
            String barcode,
            String shelfLocation,
            LocalDate acquisitionDate,
            Integer userId
    ) {
        BookCopyManager.addBookCopy(this, barcode, shelfLocation, acquisitionDate, userId);
    }

    public void updateBookCopy(
            Long copyId,
            String barcode,
            String shelfLocation,
            LocalDate acquisitionDate,
            BookCopy.CopyStatus copyStatus,
            Integer userId
    ) {
        BookCopyManager.updateBookCopy(this, copyId, barcode, shelfLocation, acquisitionDate, copyStatus, userId);
    }

    public void removeBookCopy(Long copyId, Integer userId) {
        BookCopyManager.removeBookCopy(this, copyId, userId);
    }

    public void updateCopyShelfLocation(Long copyId, String shelfLocation, Integer userId) {
        BookCopyManager.updateCopyShelfLocation(this, copyId, shelfLocation, userId);
    }

    public void updateCopyStatus(Long copyId, BookCopy.CopyStatus copyStatus, Integer userId) {
        BookCopyManager.updateCopyStatus(this, copyId, copyStatus, userId);
    }

    public void addDigitalResource(
            String fileFormat,
            String resourceUrl,
            String accessPermission,
            Integer userId
    ) {
        DigitalResourceManager.addDigitalResource(this, fileFormat, resourceUrl, accessPermission, userId);
    }

    public void updateDigitalResource(
            Long resourceId,
            String fileFormat,
            String resourceUrl,
            String accessPermission,
            Integer userId
    ) {
        DigitalResourceManager.updateDigitalResource(this, resourceId, fileFormat, resourceUrl, accessPermission, userId);
    }

    public void removeDigitalResource(Long resourceId, Integer userId) {
        DigitalResourceManager.removeDigitalResource(this, resourceId, userId);
    }

    public void updateDigitalResourceAccessPermission(
            Long resourceId,
            String accessPermission,
            Integer userId
    ) {
        DigitalResourceManager.updateDigitalResourceAccessPermission(this, resourceId, accessPermission, userId);
    }

    public DigitalResource accessDigitalResource(Long resourceId, String requesterPermission) {
        return DigitalResourceManager.accessDigitalResource(this, resourceId, requesterPermission);
    }

    void addAuditLog(BookAuditLog.AuditAction action, Integer userId) {
        if (action == null) {
            throw new IllegalArgumentException("Audit action cannot be null");
        }

        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }

        BookAuditLog log = BookAuditLog.builder()
                .action(action)
                .userId(userId)
                .changedAt(LocalDateTime.now())
                .book(book)
                .build();

        auditLogs.add(log);
    }

    public int getTotalCopies() {
        return bookCopies.size();
    }

    public int getAvailableCopies() {
        return (int) bookCopies.stream()
                .filter(copy -> copy.getCopyStatus() == BookCopy.CopyStatus.AVAILABLE)
                .count();
    }

    public Long getBookId() {
        return book != null ? book.getBookId() : null;
    }

    public Book getBook() {
        return book;
    }

    public Isbn getIsbn() {
        return isbn;
    }

    public PublicationInfo getPublicationInfo() {
        return publicationInfo;
    }

    public BookContent getBookContent() {
        return bookContent;
    }

    public List<BookCopy> getBookCopies() {
        return Collections.unmodifiableList(bookCopies);
    }

    public List<DigitalResource> getDigitalResources() {
        return Collections.unmodifiableList(digitalResources);
    }

    public List<BookAuditLog> getAuditLogs() {
        return Collections.unmodifiableList(auditLogs);
    }

    List<BookCopy> mutableBookCopies() {
        return bookCopies;
    }

    List<DigitalResource> mutableDigitalResources() {
        return digitalResources;
    }

    private static void validateTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Book title cannot be empty");
        }
    }

    private static void validateAuthor(String author) {
        if (author == null || author.trim().isEmpty()) {
            throw new IllegalArgumentException("Author cannot be empty");
        }
    }

    private static Book.BookStatus parseBookStatus(String availabilityStatus) {
        if (availabilityStatus == null || availabilityStatus.trim().isEmpty()) {
            throw new IllegalArgumentException("Availability status cannot be empty");
        }

        try {
            return Book.BookStatus.valueOf(availabilityStatus.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid book status: " + availabilityStatus, exception);
        }
    }

    private static void validateBookDetails(
            Isbn isbn,
            PublicationInfo publicationInfo,
            BookContent bookContent,
            Category category,
            Classification classification
    ) {
        if (isbn == null) {
            throw new IllegalArgumentException("ISBN cannot be null");
        }

        if (publicationInfo == null) {
            throw new IllegalArgumentException("Publication info cannot be null");
        }

        if (bookContent == null) {
            throw new IllegalArgumentException("Book content cannot be null");
        }

        if (category == null) {
            throw new IllegalArgumentException("Category cannot be null");
        }

        if (classification == null) {
            throw new IllegalArgumentException("Classification cannot be null");
        }
    }
}
