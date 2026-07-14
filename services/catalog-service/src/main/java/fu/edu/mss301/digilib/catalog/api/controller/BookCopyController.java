package fu.edu.mss301.digilib.catalog.api.controller;

import fu.edu.mss301.digilib.catalog.api.dto.BookCopyRequest;
import fu.edu.mss301.digilib.catalog.api.dto.BookCopyResponse;
import fu.edu.mss301.digilib.catalog.api.dto.BookCopyStatusRequest;
import fu.edu.mss301.digilib.catalog.api.dto.BookResponse;
import fu.edu.mss301.digilib.catalog.api.dto.ShelfLocationRequest;
import fu.edu.mss301.digilib.catalog.application.command.BookCopyCommand;
import fu.edu.mss301.digilib.catalog.application.usecase.ManageBookCopyUseCase;
import fu.edu.mss301.digilib.catalog.domain.entity.Book;
import fu.edu.mss301.digilib.catalog.domain.entity.BookCopy;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/catalog")
@RequiredArgsConstructor
public class BookCopyController {
    private static final Logger log = LoggerFactory.getLogger(BookCopyController.class);

    private final ManageBookCopyUseCase manageBookCopyUseCase;

    @GetMapping("/book-copies")
    public Page<BookCopyResponse> getBookCopies(Pageable pageable) {
        return manageBookCopyUseCase.findAll(pageable)
                .map(BookCopyResponse::from);
    }

    @GetMapping("/book-copies/deleted")
    public Page<BookCopyResponse> getDeletedBookCopies(Pageable pageable) {
        return manageBookCopyUseCase.findDeleted(pageable)
                .map(BookCopyResponse::from);
    }

    @GetMapping("/book-copies/{copyId}")
    public BookCopyResponse getBookCopy(@PathVariable Long copyId) {
        return BookCopyResponse.from(manageBookCopyUseCase.findById(copyId));
    }

    @PostMapping("/books/{bookId}/copies")
    public BookResponse addBookCopy(
            @PathVariable Long bookId,
            @RequestBody BookCopyRequest request) {
        try {
            return BookResponse.from(
                    manageBookCopyUseCase.add(new BookCopyCommand(
                            bookId,
                            null,
                            request.getBarcode(),
                            request.getShelfLocation(),
                            request.getAcquisitionDate(),
                            request.getCopyStatus(),
                            null,
                            request.getUserId())).getBook());
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        } catch (RuntimeException exception) {
            log.error("Could not add book copy for bookId={} and userId={}", bookId, request.getUserId(), exception);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not add book copy: " + exception.getMessage(),
                    exception);
        }
    }

    @PutMapping("/book-copies/{copyId}")
    public BookResponse updateBookCopy(
            @PathVariable Long copyId,
            @RequestBody BookCopyRequest request) {
        try {
            Long bookId = getBookId(manageBookCopyUseCase.findById(copyId));
            return BookResponse.from(
                    manageBookCopyUseCase.update(new BookCopyCommand(
                            bookId,
                            copyId,
                            request.getBarcode(),
                            request.getShelfLocation(),
                            request.getAcquisitionDate(),
                            request.getCopyStatus(),
                            null,
                            request.getUserId())).getBook());
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        } catch (RuntimeException exception) {
            log.error("Could not update book copyId={} and userId={}", copyId, request.getUserId(), exception);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not update book copy: " + exception.getMessage(),
                    exception);
        }
    }

    @DeleteMapping("/book-copies/{copyId}")
    public BookResponse deleteBookCopy(
            @PathVariable Long copyId,
            @RequestParam(required = false) Integer userId) {
        try {
            Long bookId = getBookId(manageBookCopyUseCase.findById(copyId));
            return BookResponse.from(
                    manageBookCopyUseCase.delete(new BookCopyCommand(bookId, copyId, null, null, null,
                            null, null, userId)).getBook());
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        } catch (RuntimeException exception) {
            log.error("Could not soft delete book copyId={} and userId={}", copyId, userId, exception);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not soft delete book copy: " + exception.getMessage(),
                    exception);
        }
    }

    @PutMapping("/book-copies/{copyId}/restore")
    public BookResponse restoreBookCopy(
            @PathVariable Long copyId,
            @RequestParam(required = false) Integer userId) {
        try {
            return BookResponse.from(
                    manageBookCopyUseCase.restore(new BookCopyCommand(null, copyId, null, null, null,
                            null, null, userId)).getBook());
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        } catch (RuntimeException exception) {
            log.error("Could not restore book copyId={} and userId={}", copyId, userId, exception);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not restore book copy: " + exception.getMessage(),
                    exception);
        }
    }

    @GetMapping("/books/{bookId}/copies")
    public Page<BookCopyResponse> getBookCopiesByBook(@PathVariable Long bookId, Pageable pageable) {
        return manageBookCopyUseCase.findByBook(bookId, pageable)
                .map(BookCopyResponse::from);
    }

    @PatchMapping("/book-copies/{copyId}/status")
    public BookResponse updateBookCopyStatus(
            @PathVariable Long copyId,
            @RequestBody BookCopyStatusRequest request) {
        try {
            Long bookId = getBookId(manageBookCopyUseCase.findById(copyId));
            return BookResponse.from(
                    manageBookCopyUseCase.updateStatus(new BookCopyCommand(
                            bookId,
                            copyId,
                            null,
                            null,
                            null,
                            request.getCopyStatus(),
                            null,
                            request.getUserId())).getBook());
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        } catch (RuntimeException exception) {
            log.error("Could not update status for book copyId={} and userId={}", copyId, request.getUserId(), exception);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not update book copy status: " + exception.getMessage(),
                    exception);
        }
    }

    @GetMapping("/book-copies/search")
    public Page<BookCopyResponse> searchBookCopies(@RequestParam String keyword, Pageable pageable) {
        return manageBookCopyUseCase.search(
                        new BookCopyCommand(null, null, null, null, null, null, keyword, null),
                        pageable
                )
                .map(BookCopyResponse::from);
    }

    @GetMapping("/book-copies/filter")
    public Page<BookCopyResponse> filterBookCopies(
            @RequestParam(required = false) BookCopy.CopyStatus copyStatus,
            @RequestParam(required = false) String shelfLocation,
            Pageable pageable) {
        return manageBookCopyUseCase.filter(new BookCopyCommand(null, null, null, shelfLocation, null,
                copyStatus, null, null), pageable)
                .map(BookCopyResponse::from);
    }

    @PatchMapping("/book-copies/{copyId}/shelf-location")
    public BookResponse updateShelfLocation(
            @PathVariable Long copyId,
            @RequestBody ShelfLocationRequest request) {
        try {
            Long bookId = getBookId(manageBookCopyUseCase.findById(copyId));
            return BookResponse.from(
                    manageBookCopyUseCase.updateShelfLocation(new BookCopyCommand(
                            bookId,
                            copyId,
                            null,
                            request.getShelfLocation(),
                            null,
                            null,
                            null,
                            request.getUserId())).getBook());
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        } catch (RuntimeException exception) {
            log.error("Could not update shelf location for book copyId={} and userId={}", copyId, request.getUserId(), exception);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not update book copy shelf location: " + exception.getMessage(),
                    exception);
        }
    }

    private Long getBookId(BookCopy copy) {
        Book book = copy.getBook();
        if (book == null || book.getBookId() == null) {
            throw new IllegalArgumentException("Book copy is not assigned to a book");
        }
        return book.getBookId();
    }
}
