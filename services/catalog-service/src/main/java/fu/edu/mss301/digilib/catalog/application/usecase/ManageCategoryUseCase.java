package fu.edu.mss301.digilib.catalog.application.usecase;

import fu.edu.mss301.digilib.catalog.application.command.CategoryCommand;
import fu.edu.mss301.digilib.catalog.domain.entity.Category;
import fu.edu.mss301.digilib.catalog.domain.repository.BookRepository;
import fu.edu.mss301.digilib.catalog.domain.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ManageCategoryUseCase {

    private final CategoryRepository categoryRepository;
    private final BookRepository bookRepository;

    public Category create(CategoryCommand command) {
        Category category = Category.builder()
                .categoryName(command.getCategoryName())
                .description(command.getDescription())
                .build();

        return categoryRepository.saveCategory(category);
    }

    public Category update(CategoryCommand command) {
        Category category = findById(command.getCategoryId());
        category.setCategoryName(command.getCategoryName());
        category.setDescription(command.getDescription());
        return categoryRepository.saveCategory(category);
    }

    public void delete(CategoryCommand command) {
        Category category = findById(command.getCategoryId());

        boolean hasLinkedBooks = bookRepository.findBooksByCategoryId(category.getCategoryId(), PageRequest.of(0, 1))
                .hasContent();
        if (hasLinkedBooks) {
            throw new IllegalArgumentException("Không thể xoá danh mục đang có đầu sách liên kết");
        }

        categoryRepository.deleteCategoryById(category.getCategoryId());
    }

    public Category restore(CategoryCommand command) {
        categoryRepository.findDeletedCategoryById(command.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Deleted category not found"));
        categoryRepository.restoreCategoryById(command.getCategoryId());
        return findById(command.getCategoryId());
    }

    @Transactional(readOnly = true)
    public Page<Category> findAll(Pageable pageable) {
        return categoryRepository.findAllCategories(pageable);
    }

    @Transactional(readOnly = true)
    public Page<Category> findDeleted(Pageable pageable) {
        return categoryRepository.findDeletedCategories(pageable);
    }

    @Transactional(readOnly = true)
    public Category findById(Long categoryId) {
        return categoryRepository.findCategoryById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));
    }

    @Transactional(readOnly = true)
    public Page<Category> search(CategoryCommand command, Pageable pageable) {
        return categoryRepository.searchCategories(command.getKeyword(), pageable);
    }
}
