package fu.edu.mss301.digilib.catalog.domain.aggregate;

import fu.edu.mss301.digilib.catalog.domain.entity.Book;
import fu.edu.mss301.digilib.catalog.domain.entity.BookAuditLog;
import fu.edu.mss301.digilib.catalog.domain.entity.BookCopy;

import java.time.LocalDate;
import java.util.List;

final class BookCopyManager {

    private BookCopyManager() {
    }

    static void addBookCopy(
            BookAggregate aggregate,
            String barcode,
            String shelfLocation,
            LocalDate acquisitionDate,
            BookCopy.CopyStatus copyStatus,
            Integer userId
    ) {
        validateBarcode(barcode);
        validateShelfLocation(shelfLocation);
        validateAcquisitionDate(acquisitionDate);
        BookCopy.CopyStatus nextCopyStatus = copyStatus == null ? BookCopy.CopyStatus.AVAILABLE : copyStatus;
        validateCopyStatus(nextCopyStatus);

        BookCopy copy = BookCopy.builder()
                .barcode(barcode)
                .shelfLocation(shelfLocation)
                .acquisitionDate(acquisitionDate)
                .copyStatus(nextCopyStatus)
                .book(aggregate.getBook())
                .build();

        aggregate.mutableBookCopies().add(copy);
        updateBookStatusFromCopies(aggregate);
        addAuditLog(aggregate, userId);
    }

    static void updateBookCopy(
            BookAggregate aggregate,
            Long copyId,
            String barcode,
            String shelfLocation,
            LocalDate acquisitionDate,
            BookCopy.CopyStatus copyStatus,
            Integer userId
    ) {
        validateBarcode(barcode);
        validateShelfLocation(shelfLocation);
        validateAcquisitionDate(acquisitionDate);
        validateCopyStatus(copyStatus);

        BookCopy copy = findCopyById(aggregate.mutableBookCopies(), copyId);
        copy.setBarcode(barcode);
        copy.setShelfLocation(shelfLocation);
        copy.setAcquisitionDate(acquisitionDate);
        copy.setCopyStatus(copyStatus);

        updateBookStatusFromCopies(aggregate);
        addAuditLog(aggregate, userId);
    }

    static void removeBookCopy(BookAggregate aggregate, Long copyId, Integer userId) {
        BookCopy copy = findCopyById(aggregate.mutableBookCopies(), copyId);
        copy.softDelete();
        updateBookStatusFromCopies(aggregate);
        addAuditLog(aggregate, userId);
    }

    static void updateCopyShelfLocation(
            BookAggregate aggregate,
            Long copyId,
            String shelfLocation,
            Integer userId
    ) {
        validateShelfLocation(shelfLocation);

        BookCopy copy = findCopyById(aggregate.mutableBookCopies(), copyId);
        copy.setShelfLocation(shelfLocation);

        addAuditLog(aggregate, userId);
    }

    static void updateCopyStatus(
            BookAggregate aggregate,
            Long copyId,
            BookCopy.CopyStatus copyStatus,
            Integer userId
    ) {
        validateCopyStatus(copyStatus);

        BookCopy copy = findCopyById(aggregate.mutableBookCopies(), copyId);
        copy.setCopyStatus(copyStatus);
        updateBookStatusFromCopies(aggregate);
        addAuditLog(aggregate, userId);
    }

    private static void updateBookStatusFromCopies(BookAggregate aggregate) {
        boolean hasUsableCopy = aggregate.mutableBookCopies().stream()
                .anyMatch(copy -> !Boolean.TRUE.equals(copy.getIsDeleted()) && copy.getCopyStatus() == BookCopy.CopyStatus.AVAILABLE);

        aggregate.getBook().setBookStatus(hasUsableCopy ? Book.BookStatus.ACTIVE : Book.BookStatus.INACTIVE);
    }

    private static BookCopy findCopyById(List<BookCopy> copies, Long copyId) {
        return copies.stream()
                .filter(copy -> copy.getCopyId() != null && copy.getCopyId().equals(copyId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Book copy not found"));
    }

    private static void addAuditLog(BookAggregate aggregate, Integer userId) {
        if (userId != null) {
            aggregate.addAuditLog(BookAuditLog.AuditAction.UPDATE, userId);
        }
    }

    private static void validateBarcode(String barcode) {
        if (barcode == null || barcode.trim().isEmpty()) {
            throw new IllegalArgumentException("Barcode cannot be empty");
        }
    }

    private static void validateShelfLocation(String shelfLocation) {
        if (shelfLocation == null || shelfLocation.trim().isEmpty()) {
            throw new IllegalArgumentException("Shelf location cannot be empty");
        }
    }

    private static void validateAcquisitionDate(LocalDate acquisitionDate) {
        if (acquisitionDate == null) {
            throw new IllegalArgumentException("Acquisition date cannot be null");
        }
    }

    private static void validateCopyStatus(BookCopy.CopyStatus copyStatus) {
        if (copyStatus == null) {
            throw new IllegalArgumentException("Copy status cannot be null");
        }
    }
}
