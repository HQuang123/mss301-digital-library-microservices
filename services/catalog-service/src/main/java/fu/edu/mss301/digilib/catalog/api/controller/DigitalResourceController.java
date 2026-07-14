package fu.edu.mss301.digilib.catalog.api.controller;

import fu.edu.mss301.digilib.catalog.api.dto.AccessPermissionRequest;
import fu.edu.mss301.digilib.catalog.api.dto.BookResponse;
import fu.edu.mss301.digilib.catalog.api.dto.DigitalResourceRequest;
import fu.edu.mss301.digilib.catalog.api.dto.DigitalResourceResponse;
import fu.edu.mss301.digilib.catalog.application.command.DigitalResourceCommand;
import fu.edu.mss301.digilib.catalog.application.usecase.ManageBookUseCase;
import fu.edu.mss301.digilib.catalog.application.usecase.ManageDigitalResourceUseCase;
import fu.edu.mss301.digilib.catalog.domain.entity.Book;
import fu.edu.mss301.digilib.catalog.domain.entity.DigitalResource;
import fu.edu.mss301.digilib.catalog.infrastructure.persistence.DigitalResourceJpaRepository;
import fu.edu.mss301.digilib.catalog.infrastructure.persistence.DigitalResourceRowProjection;
import fu.edu.mss301.digilib.catalog.infrastructure.storage.FileStorageService;
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
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/catalog")
@RequiredArgsConstructor
public class DigitalResourceController {
    private static final Logger log = LoggerFactory.getLogger(DigitalResourceController.class);

    private final ManageBookUseCase manageBookUseCase;
    private final ManageDigitalResourceUseCase manageDigitalResourceUseCase;
    private final DigitalResourceJpaRepository digitalResourceJpaRepository;
    private final FileStorageService fileStorageService;

    @GetMapping("/digital-resources")
    public Page<DigitalResourceResponse> getDigitalResources(Pageable pageable) {
        return manageDigitalResourceUseCase.findAll(pageable)
                .map(DigitalResourceResponse::from);
    }

    @GetMapping("/digital-resources/deleted")
    public Page<DigitalResourceResponse> getDeletedDigitalResources(Pageable pageable) {
        return digitalResourceJpaRepository.findDeletedResourceRows(pageable)
                .map(this::toDeletedResponse);
    }

    @GetMapping("/digital-resources/{resourceId}")
    public DigitalResourceResponse getDigitalResource(@PathVariable Long resourceId) {
        return DigitalResourceResponse.from(manageDigitalResourceUseCase.findById(resourceId));
    }

    @PostMapping("/books/{bookId}/digital-resources")
    public BookResponse addDigitalResource(
            @PathVariable Long bookId,
            @RequestBody DigitalResourceRequest request) {
        try {
            manageDigitalResourceUseCase.add(new DigitalResourceCommand(
                    bookId,
                    null,
                    request.getFileFormat(),
                    request.getResourceUrl(),
                    request.getAccessPermission(),
                    null,
                    null,
                    request.getUserId()));
            return getBookResponse(bookId);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        } catch (RuntimeException exception) {
            log.error("Could not add digital resource for bookId={}", bookId, exception);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not add digital resource: " + exception.getMessage(),
                    exception);
        }
    }

    @PostMapping(value = "/books/{bookId}/digital-resources/upload", consumes = "multipart/form-data")
    public BookResponse uploadDigitalResourcePdf(
            @PathVariable Long bookId,
            @RequestPart("file") MultipartFile file,
            @RequestParam String accessPermission,
            @RequestParam(required = false) Integer userId) {
        try {
            manageDigitalResourceUseCase.ensureBookExists(bookId);
            String resourceUrl = fileStorageService.storeDigitalResourcePdf(file, bookId);

            manageDigitalResourceUseCase.add(new DigitalResourceCommand(
                    bookId,
                    null,
                    "PDF",
                    resourceUrl,
                    accessPermission,
                    null,
                    null,
                    userId));
            return getBookResponse(bookId);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        } catch (IllegalStateException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, exception.getMessage(), exception);
        } catch (RuntimeException exception) {
            log.error("Could not upload digital resource for bookId={} and userId={}", bookId, userId, exception);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not upload digital resource: " + exception.getMessage(),
                    exception);
        }
    }

    @PutMapping(value = "/digital-resources/{resourceId}/upload", consumes = "multipart/form-data")
    public BookResponse replaceDigitalResourcePdf(
            @PathVariable Long resourceId,
            @RequestPart("file") MultipartFile file,
            @RequestParam String accessPermission,
            @RequestParam(required = false) Integer userId) {
        try {
            Long bookId = getBookId(manageDigitalResourceUseCase.findById(resourceId));
            String resourceUrl = fileStorageService.storeDigitalResourcePdf(file, bookId);

            manageDigitalResourceUseCase.update(new DigitalResourceCommand(
                    bookId,
                    resourceId,
                    "PDF",
                    resourceUrl,
                    accessPermission,
                    null,
                    null,
                    userId));
            return getBookResponse(bookId);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        } catch (IllegalStateException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, exception.getMessage(), exception);
        } catch (RuntimeException exception) {
            log.error("Could not replace digital resource file for resourceId={} and userId={}", resourceId, userId, exception);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not replace digital resource file: " + exception.getMessage(),
                    exception);
        }
    }

    @PutMapping("/digital-resources/{resourceId}")
    public BookResponse updateDigitalResource(
            @PathVariable Long resourceId,
            @RequestBody DigitalResourceRequest request) {
        try {
            Long bookId = getBookId(manageDigitalResourceUseCase.findById(resourceId));
            manageDigitalResourceUseCase.update(new DigitalResourceCommand(
                    bookId,
                    resourceId,
                    request.getFileFormat(),
                    request.getResourceUrl(),
                    request.getAccessPermission(),
                    null,
                    null,
                    request.getUserId()));
            return getBookResponse(bookId);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        } catch (RuntimeException exception) {
            log.error("Could not update digital resourceId={}", resourceId, exception);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not update digital resource: " + exception.getMessage(),
                    exception);
        }
    }

    @DeleteMapping("/digital-resources/{resourceId}")
    public BookResponse deleteDigitalResource(
            @PathVariable Long resourceId,
            @RequestParam(required = false) Integer userId) {
        Long bookId = getBookId(manageDigitalResourceUseCase.findById(resourceId));
        manageDigitalResourceUseCase.delete(
                new DigitalResourceCommand(bookId, resourceId, null, null, null, null, null, userId));
        return getBookResponse(bookId);
    }

    @PutMapping("/digital-resources/{resourceId}/restore")
    public BookResponse restoreDigitalResource(
            @PathVariable Long resourceId,
            @RequestParam(required = false) Integer userId,
            @RequestParam Long bookId) {
        try {
            manageDigitalResourceUseCase.restore(
                    new DigitalResourceCommand(bookId, resourceId, null, null, null, null, null, userId));
            return getBookResponse(bookId);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        } catch (RuntimeException exception) {
            log.error("Could not restore digital resourceId={} for bookId={} and userId={}", resourceId, bookId, userId, exception);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not restore digital resource: " + exception.getMessage(),
                    exception);
        }
    }

    @GetMapping("/books/{bookId}/digital-resources")
    public Page<DigitalResourceResponse> getDigitalResourcesByBook(@PathVariable Long bookId, Pageable pageable) {
        return manageDigitalResourceUseCase.findByBook(bookId, pageable)
                .map(DigitalResourceResponse::from);
    }

    @PatchMapping("/digital-resources/{resourceId}/permission")
    public BookResponse updateDigitalResourcePermission(
            @PathVariable Long resourceId,
            @RequestBody AccessPermissionRequest request) {
        Long bookId = getBookId(manageDigitalResourceUseCase.findById(resourceId));
        manageDigitalResourceUseCase.updateAccess(new DigitalResourceCommand(
                bookId,
                resourceId,
                null,
                null,
                request.getAccessPermission(),
                null,
                null,
                request.getUserId()));
        return getBookResponse(bookId);
    }

    @GetMapping("/digital-resources/{resourceId}/access")
    public DigitalResourceResponse accessDigitalResource(
            @PathVariable Long resourceId,
            @RequestParam(required = false) String requesterPermission) {
        Long bookId = getBookId(manageDigitalResourceUseCase.findById(resourceId));
        return DigitalResourceResponse.from(
                manageDigitalResourceUseCase.access(
                        new DigitalResourceCommand(bookId, resourceId, null, null, null, requesterPermission, null,
                                null)));
    }

    @GetMapping("/digital-resources/search")
    public Page<DigitalResourceResponse> searchDigitalResources(@RequestParam String keyword, Pageable pageable) {
        return manageDigitalResourceUseCase.search(new DigitalResourceCommand(null, null, null, null, null,
                null, keyword, null), pageable)
                .map(DigitalResourceResponse::from);
    }

    @GetMapping("/digital-resources/filter")
    public Page<DigitalResourceResponse> filterDigitalResources(
            @RequestParam(required = false) String fileFormat,
            @RequestParam(required = false) String accessPermission,
            Pageable pageable) {
        return manageDigitalResourceUseCase.filter(
                        new DigitalResourceCommand(null, null, fileFormat, null, accessPermission, null, null, null),
                        pageable
                )
                .map(DigitalResourceResponse::from);
    }

    private Long getBookId(DigitalResource resource) {
        Book book = resource.getBook();
        if (book == null || book.getBookId() == null) {
            throw new IllegalArgumentException("Digital resource is not assigned to a book");
        }
        return book.getBookId();
    }

    private BookResponse getBookResponse(Long bookId) {
        return BookResponse.from(manageBookUseCase.findById(bookId));
    }

    private DigitalResourceResponse toDeletedResponse(DigitalResourceRowProjection row) {
        return new DigitalResourceResponse(
                row.getResourceId(),
                row.getFileFormat(),
                row.getResourceUrl(),
                row.getAccessPermission(),
                row.getUploadedAt(),
                row.getBookId(),
                Boolean.TRUE.equals(row.getIsDeleted())
        );
    }
}
