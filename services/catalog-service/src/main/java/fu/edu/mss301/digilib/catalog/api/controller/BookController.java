package fu.edu.mss301.digilib.catalog.api.controller;

import fu.edu.mss301.digilib.catalog.api.dto.BookCreateRequest;
import fu.edu.mss301.digilib.catalog.api.dto.BookResponse;
import fu.edu.mss301.digilib.catalog.api.dto.BookStatusRequest;
import fu.edu.mss301.digilib.catalog.api.dto.BookUpdateRequest;
import fu.edu.mss301.digilib.catalog.application.command.BookCommand;
import fu.edu.mss301.digilib.catalog.application.usecase.ManageBookUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class BookController {

    private final ManageBookUseCase manageBookUseCase;

    @GetMapping("/books")
    public Page<BookResponse> getBooks(Pageable pageable) {
        return manageBookUseCase.findAll(pageable)
                .map(BookResponse::from);
    }

    @GetMapping("/books/{bookId}")
    public BookResponse getBook(@PathVariable Long bookId) {
        return BookResponse.from(manageBookUseCase.findById(bookId));
    }

    @PostMapping("/books")
    public ResponseEntity<BookResponse> createBook(@RequestBody BookCreateRequest request) {
        BookResponse response = BookResponse.from(
                manageBookUseCase.execute(toCreateBookCommand(request)).getBook());
        return ResponseEntity.created(URI.create("/api/books/" + response.getBookId())).body(response);
    }

    @PutMapping("/books/{bookId}")
    public BookResponse updateBook(
            @PathVariable Long bookId,
            @RequestBody BookUpdateRequest request) {
        return BookResponse.from(
                manageBookUseCase.update(toUpdateBookCommand(bookId, request)).getBook());
    }

    @DeleteMapping("/books/{bookId}")
    public BookResponse deleteBook(
            @PathVariable Long bookId,
            @RequestParam(required = false) Integer userId) {
        return BookResponse.from(
                manageBookUseCase.delete(new BookCommand(bookId, null, null, null, null, null, null, null,
                        null, null, null, null, null, null, userId)).getBook());
    }

    @GetMapping("/books/search")
    public Page<BookResponse> searchBooks(@RequestParam String keyword, Pageable pageable) {
        return manageBookUseCase.search(new BookCommand(null, null, null, null, null, null, null, null,
                null, null, null, null, null, keyword, null), pageable)
                .map(BookResponse::from);
    }

    @PutMapping("/books/{bookId}/status")
    public BookResponse updateBookStatus(
            @PathVariable Long bookId,
            @RequestBody BookStatusRequest request) {
        return BookResponse.from(
                manageBookUseCase.updateStatus(new BookCommand(
                        bookId,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        request.getAvailabilityStatus(),
                        null,
                        request.getUserId())).getBook());
    }

    private BookCommand toCreateBookCommand(BookCreateRequest request) {
        return new BookCommand(
                null,
                request.getIsbn(),
                request.getTitle(),
                request.getAuthor(),
                request.getPublisher(),
                request.getPublicationYear(),
                request.getEdition(),
                request.getLanguage(),
                request.getDescription(),
                request.getCoverImageUrl(),
                request.getCategoryId(),
                request.getClassificationId(),
                null,
                null,
                request.getUserId());
    }

    private BookCommand toUpdateBookCommand(Long bookId, BookUpdateRequest request) {
        return new BookCommand(
                bookId,
                null,
                request.getTitle(),
                request.getAuthor(),
                request.getPublisher(),
                request.getPublicationYear(),
                request.getEdition(),
                request.getLanguage(),
                request.getDescription(),
                request.getCoverImageUrl(),
                request.getCategoryId(),
                request.getClassificationId(),
                null,
                null,
                request.getUserId());
    }
}
