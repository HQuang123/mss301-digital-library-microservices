package fu.edu.mss301.digilib.fine.infrastructure.client;

import fu.edu.mss301.digilib.fine.infrastructure.client.dto.BookSummaryDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Fine Service does not own book data. This client is used only to resolve
 * a book title for display when listing fine history (see FINE_HISTORY_FLOW.md);
 * bookId itself comes from Loan Service at fine-creation time.
 */
@FeignClient(name = "catalog-service")
public interface CatalogServiceClient {

    @GetMapping("/api/catalog/books/{bookId}")
    BookSummaryDto getBook(@PathVariable("bookId") Long bookId);
}
