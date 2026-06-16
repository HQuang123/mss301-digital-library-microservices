package fu.edu.mss301.digilib.catalog.api.controller;

import fu.edu.mss301.digilib.catalog.api.dto.BookResponse;
import fu.edu.mss301.digilib.catalog.api.dto.CategoryRequest;
import fu.edu.mss301.digilib.catalog.api.dto.CategoryResponse;
import fu.edu.mss301.digilib.catalog.application.command.BookCommand;
import fu.edu.mss301.digilib.catalog.application.command.CategoryCommand;
import fu.edu.mss301.digilib.catalog.application.usecase.ManageBookUseCase;
import fu.edu.mss301.digilib.catalog.application.usecase.ManageCategoryUseCase;
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
@RequestMapping("/api/catalog")
@RequiredArgsConstructor
public class CategoryController {

    private final ManageCategoryUseCase manageCategoryUseCase;
    private final ManageBookUseCase manageBookUseCase;

    @GetMapping("/categories")
    public Page<CategoryResponse> getCategories(Pageable pageable) {
        return manageCategoryUseCase.findAll(pageable)
                .map(CategoryResponse::from);
    }

    @GetMapping("/categories/{categoryId}")
    public CategoryResponse getCategory(@PathVariable Long categoryId) {
        return CategoryResponse.from(manageCategoryUseCase.findById(categoryId));
    }

    @PostMapping("/categories")
    public ResponseEntity<CategoryResponse> createCategory(
            @RequestBody CategoryRequest request) {
        CategoryResponse response = CategoryResponse.from(
                manageCategoryUseCase.create(new CategoryCommand(
                        null,
                        request.getCategoryName(),
                        request.getDescription(),
                        null)));
        return ResponseEntity.created(URI.create("/api/categories/" + response.getCategoryId())).body(response);
    }

    @PutMapping("/categories/{categoryId}")
    public CategoryResponse updateCategory(
            @PathVariable Long categoryId,
            @RequestBody CategoryRequest request) {
        return CategoryResponse.from(
                manageCategoryUseCase.update(new CategoryCommand(
                        categoryId,
                        request.getCategoryName(),
                        request.getDescription(),
                        null)));
    }

    @DeleteMapping("/categories/{categoryId}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long categoryId) {
        manageCategoryUseCase.delete(new CategoryCommand(categoryId, null, null, null));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/categories/search")
    public Page<CategoryResponse> searchCategories(@RequestParam String keyword, Pageable pageable) {
        return manageCategoryUseCase.search(new CategoryCommand(null, null, null, keyword), pageable)
                .map(CategoryResponse::from);
    }

    @GetMapping("/categories/{categoryId}/books")
    public Page<BookResponse> getBooksByCategory(@PathVariable Long categoryId, Pageable pageable) {
        return manageBookUseCase.findByCategory(categoryId, pageable)
                .map(BookResponse::from);
    }

    @PutMapping("/books/{bookId}/category/{categoryId}")
    public BookResponse assignCategoryToBook(
            @PathVariable Long bookId,
            @PathVariable Long categoryId,
            @RequestParam(required = false) Integer userId) {
        return BookResponse.from(
                manageBookUseCase.assignCategory(
                        new BookCommand(bookId, null, null, null, null, null, null, null, null, null,
                                categoryId, null, null, null, userId))
                        .getBook());
    }
}
