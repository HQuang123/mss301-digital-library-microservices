package fu.edu.mss301.digilib.catalog.api.controller;

import fu.edu.mss301.digilib.catalog.api.dto.AccessPermissionRequest;
import fu.edu.mss301.digilib.catalog.api.dto.BookResponse;
import fu.edu.mss301.digilib.catalog.api.dto.DigitalResourceRequest;
import fu.edu.mss301.digilib.catalog.api.dto.DigitalResourceResponse;
import fu.edu.mss301.digilib.catalog.application.command.DigitalResourceCommand;
import fu.edu.mss301.digilib.catalog.application.usecase.ManageDigitalResourceUseCase;
import fu.edu.mss301.digilib.catalog.domain.entity.Book;
import fu.edu.mss301.digilib.catalog.domain.entity.DigitalResource;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

@RestController
@RequestMapping("/api/catalog")
@RequiredArgsConstructor
public class DigitalResourceController {

    private final ManageDigitalResourceUseCase manageDigitalResourceUseCase;

    @GetMapping("/digital-resources")
    public Page<DigitalResourceResponse> getDigitalResources(Pageable pageable) {
        return manageDigitalResourceUseCase.findAll(pageable)
                .map(DigitalResourceResponse::from);
    }

    @GetMapping("/digital-resources/{resourceId}")
    public DigitalResourceResponse getDigitalResource(@PathVariable Long resourceId) {
        return DigitalResourceResponse.from(manageDigitalResourceUseCase.findById(resourceId));
    }

    @PostMapping("/books/{bookId}/digital-resources")
    public BookResponse addDigitalResource(
            @PathVariable Long bookId,
            @RequestBody DigitalResourceRequest request) {
        return BookResponse.from(
                manageDigitalResourceUseCase.add(new DigitalResourceCommand(
                        bookId,
                        null,
                        request.getFileFormat(),
                        request.getResourceUrl(),
                        request.getAccessPermission(),
                        null,
                        null,
                        request.getUserId())).getBook());
    }

    @PutMapping("/digital-resources/{resourceId}")
    public BookResponse updateDigitalResource(
            @PathVariable Long resourceId,
            @RequestBody DigitalResourceRequest request) {
        Long bookId = getBookId(manageDigitalResourceUseCase.findById(resourceId));
        return BookResponse.from(
                manageDigitalResourceUseCase.update(new DigitalResourceCommand(
                        bookId,
                        resourceId,
                        request.getFileFormat(),
                        request.getResourceUrl(),
                        request.getAccessPermission(),
                        null,
                        null,
                        request.getUserId())).getBook());
    }

    @DeleteMapping("/digital-resources/{resourceId}")
    public BookResponse deleteDigitalResource(
            @PathVariable Long resourceId,
            @RequestParam(required = false) Integer userId) {
        Long bookId = getBookId(manageDigitalResourceUseCase.findById(resourceId));
        return BookResponse.from(
                manageDigitalResourceUseCase.delete(
                        new DigitalResourceCommand(bookId, resourceId, null, null, null, null, null, userId))
                        .getBook());
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
        return BookResponse.from(
                manageDigitalResourceUseCase.updateAccess(new DigitalResourceCommand(
                        bookId,
                        resourceId,
                        null,
                        null,
                        request.getAccessPermission(),
                        null,
                        null,
                        request.getUserId())).getBook());
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
}
