package fu.edu.mss301.digilib.catalog.domain.repository;

import fu.edu.mss301.digilib.catalog.domain.entity.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface CategoryRepository {

    Category saveCategory(Category category);

    Page<Category> findAllCategories(Pageable pageable);

    Page<Category> findDeletedCategories(Pageable pageable);

    Optional<Category> findCategoryById(Long categoryId);

    Optional<Category> findDeletedCategoryById(Long categoryId);

    Page<Category> searchCategories(String keyword, Pageable pageable);

    void deleteCategoryById(Long categoryId);

    void restoreCategoryById(Long categoryId);
}
