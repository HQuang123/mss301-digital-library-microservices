package fu.edu.mss301.digilib.catalog.infrastructure.persistence;

import fu.edu.mss301.digilib.catalog.domain.entity.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryJpaRepository extends JpaRepository<Category, Long> {

    Page<Category> findByCategoryNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
            String categoryName,
            String description,
            Pageable pageable
    );
}
