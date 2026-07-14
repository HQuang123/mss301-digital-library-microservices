package fu.edu.mss301.digilib.catalog.api.controller;

import fu.edu.mss301.digilib.catalog.api.dto.BookCreateRequest;
import fu.edu.mss301.digilib.catalog.api.dto.BookResponse;
import fu.edu.mss301.digilib.catalog.api.dto.BookStatusRequest;
import fu.edu.mss301.digilib.catalog.api.dto.BookUpdateRequest;
import fu.edu.mss301.digilib.catalog.application.command.BookCommand;
import fu.edu.mss301.digilib.catalog.application.usecase.ManageBookUseCase;
import fu.edu.mss301.digilib.catalog.domain.entity.Book;
import fu.edu.mss301.digilib.catalog.infrastructure.storage.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.Map;
@RestController
@RequestMapping("/api/catalog")
@RequiredArgsConstructor
public class BookController {
    private static final Logger log = LoggerFactory.getLogger(BookController.class);

    private final ManageBookUseCase manageBookUseCase;
    private final FileStorageService fileStorageService;

    @GetMapping("/books")
    public Page<BookResponse> getBooks(Pageable pageable) {
        return manageBookUseCase.findAll(pageable)
                .map(BookResponse::from);
    }

    @GetMapping("/books/deleted")
    public Page<BookResponse> getDeletedBooks(Pageable pageable) {
        return manageBookUseCase.findDeleted(pageable)
                .map(BookResponse::from);
    }

    @GetMapping("/books/{bookId}")
    public BookResponse getBook(@PathVariable("bookId") Long bookId) {
        return BookResponse.from(manageBookUseCase.findById(bookId));
    }

    @GetMapping("/books/{bookId}/cover")
    public ResponseEntity<Resource> getBookCover(@PathVariable("bookId") Long bookId) {
        String coverImageUrl = null;

        try {
            Book book = manageBookUseCase.findById(bookId);
            coverImageUrl = book.getCoverImageUrl();
        } catch (Exception ignored) {
        }

        try {
            Resource resource = fileStorageService.loadBookCover(bookId, coverImageUrl);
            MediaType mediaType = fileStorageService.detectMediaType(resource);
            return ResponseEntity.ok().contentType(mediaType).body(resource);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, exception.getMessage(), exception);
        }
    }

    @PostMapping("/books")
    public ResponseEntity<BookResponse> createBook(@RequestBody BookCreateRequest request) {
        BookResponse response = BookResponse.from(
                manageBookUseCase.execute(toCreateBookCommand(request)).getBook());
        return ResponseEntity.created(URI.create("/api/books/" + response.getBookId())).body(response);
    }

    @PutMapping("/books/{bookId}")
    public BookResponse updateBook(
            @PathVariable("bookId") Long bookId,
            @RequestBody BookUpdateRequest request) {
        return BookResponse.from(
                manageBookUseCase.update(toUpdateBookCommand(bookId, request)).getBook());
    }

    @DeleteMapping("/books/{bookId}")
    public BookResponse deleteBook(
            @PathVariable("bookId") Long bookId,
            @RequestParam(name = "userId", required = false) Integer userId) {
        try {
            return BookResponse.from(
                    manageBookUseCase.delete(new BookCommand(bookId, null, null, null, null, null, null, null,
                            null, null, null, null, null, null, userId)).getBook());
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        } catch (RuntimeException exception) {
            log.error("Could not soft delete bookId={} and userId={}", bookId, userId, exception);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not soft delete book: " + exception.getMessage(),
                    exception);
        }
    }

    @PutMapping("/books/{bookId}/restore")
    public BookResponse restoreBook(
            @PathVariable("bookId") Long bookId,
            @RequestParam(name = "userId", required = false) Integer userId) {
        try {
            return BookResponse.from(
                    manageBookUseCase.restore(new BookCommand(bookId, null, null, null, null, null, null, null,
                            null, null, null, null, null, null, userId)).getBook());
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        } catch (RuntimeException exception) {
            log.error("Could not restore soft deleted bookId={} and userId={}", bookId, userId, exception);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not restore book: " + exception.getMessage(),
                    exception);
        }
    }

    @GetMapping("/books/search")
    public Page<BookResponse> searchBooks(@RequestParam("keyword") String keyword, Pageable pageable) {
        return manageBookUseCase.search(new BookCommand(null, null, null, null, null, null, null, null,
                null, null, null, null, null, keyword, null), pageable)
                .map(BookResponse::from);
    }

    @PutMapping("/books/{bookId}/status")
    public BookResponse updateBookStatus(
            @PathVariable("bookId") Long bookId,
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

    @PostMapping(value = "/books/{bookId}/cover", consumes = "multipart/form-data")
    public ResponseEntity<Map<String, Object>> uploadBookCover(
            @PathVariable("bookId") Long bookId,
            @RequestPart("file") MultipartFile file,
            @RequestParam(name = "userId", required = false) Integer userId) {
        manageBookUseCase.findById(bookId);

        try {
            String coverImageUrl = fileStorageService.storeBookCover(file, bookId);
            manageBookUseCase.updateCoverImage(new BookCommand(
                    bookId,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    coverImageUrl,
                    null,
                    null,
                    null,
                    null,
                    userId));

            return ResponseEntity.ok(Map.of(
                    "bookId", bookId,
                    "coverImageUrl", coverImageUrl
            ));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        } catch (IllegalStateException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, exception.getMessage(), exception);
        } catch (RuntimeException exception) {
            log.error("Could not upload cover for bookId={} and userId={}", bookId, userId, exception);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not upload book cover: " + exception.getMessage(),
                    exception);
        }
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
