package fu.edu.mss301.digilib.catalog.infrastructure.persistence;

import fu.edu.mss301.digilib.catalog.domain.entity.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CategoryJpaRepository extends JpaRepository<Category, Long> {

    @Query(
            value = "select * from categories where is_deleted = true",
            countQuery = "select count(*) from categories where is_deleted = true",
            nativeQuery = true
    )
    Page<Category> findDeleted(Pageable pageable);

    @Query(value = "select * from categories where category_id = :categoryId and is_deleted = true", nativeQuery = true)
    Optional<Category> findDeletedById(@Param("categoryId") Long categoryId);

    Page<Category> findByCategoryNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
            String categoryName,
            String description,
            Pageable pageable
    );

    @Modifying
    @Query(value = "update categories set is_deleted = true where category_id = :categoryId", nativeQuery = true)
    void softDeleteById(@Param("categoryId") Long categoryId);

    @Modifying
    @Query(value = "update categories set is_deleted = false where category_id = :categoryId", nativeQuery = true)
    void restoreById(@Param("categoryId") Long categoryId);
}
