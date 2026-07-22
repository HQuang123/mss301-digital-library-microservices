package fu.edu.mss301.digilib.loan.infrastructure.adapter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
public class BookCatalogClientAdapter {
    private final RestClient restClient;
    private final long defaultBookValue;

    public BookCatalogClientAdapter(
            RestClient.Builder restClientBuilder,
            @Value("${services.catalog.base-url}") String bookCatalogBaseUrl,
            @Value("${services.catalog.default-book-value:250000}") long defaultBookValue
    ) {
        this.defaultBookValue = defaultBookValue;
        this.restClient = restClientBuilder
                .baseUrl(bookCatalogBaseUrl)
                .build();
    }

    public Long reserveBook(Long bookId) {
        BookCopyPage response = restClient.get()
                .uri("/api/catalog/books/{bookId}/copies?size=100", bookId)
                .retrieve()
                .body(BookCopyPage.class);

        BookCopy availableCopy = response == null || response.content() == null
                ? null
                : response.content().stream()
                    .filter(copy -> "AVAILABLE".equalsIgnoreCase(copy.copyStatus()))
                    .findFirst()
                    .orElse(null);
        if (availableCopy == null) {
            throw new IllegalStateException("Sách hiện không còn bản sao có thể mượn");
        }

        updateStatus(availableCopy.copyId(), "BORROWED");
        return availableCopy.copyId();
    }

    public void releaseBook(Long copyId) {
        if (copyId == null) {
            return;
        }
        updateStatus(copyId, "AVAILABLE");
    }

    public void markLost(Long copyId) {
        if (copyId == null) {
            return;
        }
        updateStatus(copyId, "LOST");
    }

    public BookDetails getBookDetails(Long bookId) {
        BookResponse response = restClient.get()
                .uri("/api/catalog/books/{bookId}", bookId)
                .retrieve()
                .body(BookResponse.class);
        if (response == null) {
            throw new IllegalStateException("Dịch vụ danh mục không trả về thông tin sách");
        }
        long bookValue = response.bookValue() == null || response.bookValue() <= 0
                ? defaultBookValue
                : response.bookValue();
        return new BookDetails(
                response.bookId(),
                response.title() == null || response.title().isBlank() ? "Sách " + bookId : response.title(),
                bookValue);
    }

    private void updateStatus(Long copyId, String status) {
        restClient.patch()
                .uri("/api/catalog/book-copies/{copyId}/status", copyId)
                .body(Map.of("copyStatus", status))
                .retrieve()
                .toBodilessEntity();
    }

    private record BookCopyPage(List<BookCopy> content) {}
    private record BookCopy(Long copyId, String copyStatus) {}
    private record BookResponse(Long bookId, String title, Long bookValue) {}
    public record BookDetails(Long bookId, String title, long bookValue) {}
}
