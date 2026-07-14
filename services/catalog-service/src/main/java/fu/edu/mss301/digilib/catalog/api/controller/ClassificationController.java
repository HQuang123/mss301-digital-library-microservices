package fu.edu.mss301.digilib.catalog.api.controller;

import fu.edu.mss301.digilib.catalog.api.dto.BookResponse;
import fu.edu.mss301.digilib.catalog.api.dto.ClassificationRequest;
import fu.edu.mss301.digilib.catalog.api.dto.ClassificationResponse;
import fu.edu.mss301.digilib.catalog.application.command.BookCommand;
import fu.edu.mss301.digilib.catalog.application.command.ClassificationCommand;
import fu.edu.mss301.digilib.catalog.application.usecase.ManageBookUseCase;
import fu.edu.mss301.digilib.catalog.application.usecase.ManageClassificationUseCase;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
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
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
@RestController
@RequestMapping("/api/catalog")
@RequiredArgsConstructor
public class ClassificationController {
    private static final Logger log = LoggerFactory.getLogger(ClassificationController.class);

    private final ManageClassificationUseCase manageClassificationUseCase;
    private final ManageBookUseCase manageBookUseCase;

    @GetMapping("/classifications")
    public Page<ClassificationResponse> getClassifications(Pageable pageable) {
        return manageClassificationUseCase.findAll(pageable)
                .map(ClassificationResponse::from);
    }

    @GetMapping("/classifications/deleted")
    public Page<ClassificationResponse> getDeletedClassifications(Pageable pageable) {
        return manageClassificationUseCase.findDeleted(pageable)
                .map(ClassificationResponse::from);
    }

    @GetMapping("/classifications/{classificationId}")
    public ClassificationResponse getClassification(@PathVariable Long classificationId) {
        return ClassificationResponse.from(manageClassificationUseCase.findById(classificationId));
    }

    @PostMapping("/classifications")
    public ResponseEntity<ClassificationResponse> createClassification(
            @RequestBody ClassificationRequest request) {
        ClassificationResponse response = ClassificationResponse.from(
                manageClassificationUseCase.create(new ClassificationCommand(
                        null,
                        request.getClassificationSystem(),
                        request.getClassificationName(),
                        request.getClassificationCode(),
                        null)));
        return ResponseEntity.created(URI.create("/api/classifications/" + response.getClassificationId()))
                .body(response);
    }

    @PutMapping("/classifications/{classificationId}")
    public ClassificationResponse updateClassification(
            @PathVariable Long classificationId,
            @RequestBody ClassificationRequest request) {
        return ClassificationResponse.from(
                manageClassificationUseCase.update(new ClassificationCommand(
                        classificationId,
                        request.getClassificationSystem(),
                        request.getClassificationName(),
                        request.getClassificationCode(),
                        null)));
    }

    @DeleteMapping("/classifications/{classificationId}")
    public ResponseEntity<Void> deleteClassification(@PathVariable Long classificationId) {
        try {
            manageClassificationUseCase.delete(new ClassificationCommand(classificationId, null, null, null, null));
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        } catch (RuntimeException exception) {
            log.error("Could not soft delete classificationId={}", classificationId, exception);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not soft delete classification: " + exception.getMessage(),
                    exception);
        }
    }

    @PutMapping("/classifications/{classificationId}/restore")
    public ClassificationResponse restoreClassification(@PathVariable Long classificationId) {
        try {
            return ClassificationResponse.from(
                    manageClassificationUseCase.restore(new ClassificationCommand(classificationId, null, null, null, null)));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        } catch (RuntimeException exception) {
            log.error("Could not restore classificationId={}", classificationId, exception);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not restore classification: " + exception.getMessage(),
                    exception);
        }
    }

    @GetMapping("/classifications/search")
    public Page<ClassificationResponse> searchClassifications(@RequestParam String keyword, Pageable pageable) {
        return manageClassificationUseCase.search(
                        new ClassificationCommand(null, null, null, null, keyword),
                        pageable
                )
                .map(ClassificationResponse::from);
    }

    @GetMapping("/classifications/{classificationId}/books")
    public Page<BookResponse> getBooksByClassification(@PathVariable Long classificationId, Pageable pageable) {
        return manageBookUseCase.findByClassification(classificationId, pageable)
                .map(BookResponse::from);
    }

    @PutMapping("/books/{bookId}/classification/{classificationId}")
    public BookResponse assignClassificationToBook(
            @PathVariable Long bookId,
            @PathVariable Long classificationId,
            @RequestParam(required = false) Integer userId) {
        return BookResponse.from(
                manageBookUseCase.assignClassification(
                        new BookCommand(bookId, null, null, null, null, null, null, null, null, null,
                                null, classificationId, null, null, userId))
                        .getBook());
    }
}
