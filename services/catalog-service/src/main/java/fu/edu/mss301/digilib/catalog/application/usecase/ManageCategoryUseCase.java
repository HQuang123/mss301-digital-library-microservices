package fu.edu.mss301.digilib.catalog.application.usecase;

import fu.edu.mss301.digilib.catalog.application.command.CategoryCommand;
import fu.edu.mss301.digilib.catalog.domain.entity.Category;
import fu.edu.mss301.digilib.catalog.domain.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ManageCategoryUseCase {

    private final CategoryRepository categoryRepository;

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
        categoryRepository.deleteCategoryById(command.getCategoryId());
    }

    @Transactional(readOnly = true)
    public Page<Category> findAll(Pageable pageable) {
        return categoryRepository.findAllCategories(pageable);
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
