package fu.edu.mss301.digilib.catalog.domain.repository;

import java.util.List;

public interface InventoryRepository {

    InventoryOverview getInventoryOverview();

    List<BookCopyCount> countBookCopiesByBook();

    InventoryStatus checkBookInventoryStatus(Long bookId);

    InventoryReport getInventoryReport();

    record InventoryOverview(
            long totalBooks,
            long totalCopies,
            long availableCopies,
            long borrowedCopies,
            long reservedCopies,
            long lostCopies,
            long damagedCopies,
            long digitalResources
    ) {
    }

    record BookCopyCount(
            Long bookId,
            String title,
            long totalCopies,
            long availableCopies
    ) {
    }

    record InventoryStatus(
            Long bookId,
            String title,
            String availabilityStatus,
            long totalCopies,
            long availableCopies
    ) {
    }

    record InventoryReport(
            InventoryOverview overview,
            List<BookCopyCount> copyCounts,
            List<InventoryStatus> statuses
    ) {
    }
}
