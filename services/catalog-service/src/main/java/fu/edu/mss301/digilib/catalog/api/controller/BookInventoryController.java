package fu.edu.mss301.digilib.catalog.api.controller;

import fu.edu.mss301.digilib.catalog.api.dto.BookResponse;
import fu.edu.mss301.digilib.catalog.application.usecase.ViewInventoryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/catalog")
@RequiredArgsConstructor
public class BookInventoryController {

    private final ViewInventoryUseCase viewInventoryUseCase;

    @GetMapping("/book-inventory/overview")
    public Object getInventoryOverview() {
        return viewInventoryUseCase.getOverview();
    }

    @GetMapping("/books/{bookId}/inventory")
    public Object getBookInventory(@PathVariable Long bookId) {
        return viewInventoryUseCase.checkStatus(bookId);
    }

    @GetMapping("/book-inventory/status/{status}")
    public Page<BookResponse> getBooksByInventoryStatus(@PathVariable String status, Pageable pageable) {
        return viewInventoryUseCase.findBooksByStatus(status, pageable)
                .map(BookResponse::from);
    }

    @GetMapping("/book-inventory/reports")
    public Object getInventoryReport() {
        return viewInventoryUseCase.getReport();
    }
}
