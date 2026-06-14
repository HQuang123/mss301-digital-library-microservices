package fu.edu.mss301.digilib.catalog.application.usecase;

import fu.edu.mss301.digilib.catalog.domain.repository.BookRepository;
import fu.edu.mss301.digilib.catalog.domain.entity.Book;
import fu.edu.mss301.digilib.catalog.domain.repository.InventoryRepository;
import fu.edu.mss301.digilib.catalog.domain.repository.InventoryRepository.BookCopyCount;
import fu.edu.mss301.digilib.catalog.domain.repository.InventoryRepository.InventoryOverview;
import fu.edu.mss301.digilib.catalog.domain.repository.InventoryRepository.InventoryReport;
import fu.edu.mss301.digilib.catalog.domain.repository.InventoryRepository.InventoryStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ViewInventoryUseCase {

    private final BookRepository bookRepository;
    private final InventoryRepository inventoryRepository;

    public InventoryOverview getOverview() {
        return inventoryRepository.getInventoryOverview();
    }

    public List<BookCopyCount> countCopiesByBook() {
        return inventoryRepository.countBookCopiesByBook();
    }

    public InventoryStatus checkStatus(Long bookId) {
        return inventoryRepository.checkBookInventoryStatus(bookId);
    }

    public Page<Book> findBooksByStatus(String status, Pageable pageable) {
        return bookRepository.findBooksByAvailabilityStatus(status, pageable);
    }

    public InventoryReport getReport() {
        return inventoryRepository.getInventoryReport();
    }
}
