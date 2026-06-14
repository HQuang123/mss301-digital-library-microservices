package fu.edu.mss301.digilib.catalog.infrastructure.adapter;

import fu.edu.mss301.digilib.catalog.domain.entity.Category;
import fu.edu.mss301.digilib.catalog.domain.repository.CategoryRepository;
import fu.edu.mss301.digilib.catalog.infrastructure.persistence.CategoryJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CategoryRepositoryAdapter implements CategoryRepository {

    private final CategoryJpaRepository categoryJpaRepository;

    @Override
    public Category saveCategory(Category category) {
        return categoryJpaRepository.save(category);
    }

    @Override
    public Page<Category> findAllCategories(Pageable pageable) {
        return categoryJpaRepository.findAll(pageable);
    }

    @Override
    public Optional<Category> findCategoryById(Long categoryId) {
        return categoryJpaRepository.findById(categoryId);
    }

    @Override
    public Page<Category> searchCategories(String keyword, Pageable pageable) {
        return categoryJpaRepository.findByCategoryNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
                keyword,
                keyword,
                pageable
        );
    }

    @Override
    public void deleteCategoryById(Long categoryId) {
        categoryJpaRepository.deleteById(categoryId);
    }
}
