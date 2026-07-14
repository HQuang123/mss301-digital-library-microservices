package fu.edu.mss301.digilib.catalog.infrastructure.persistence;

import fu.edu.mss301.digilib.catalog.domain.entity.Classification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ClassificationJpaRepository extends JpaRepository<Classification, Long> {

    @Query(
            value = "select * from classifications where is_deleted = true",
            countQuery = "select count(*) from classifications where is_deleted = true",
            nativeQuery = true
    )
    Page<Classification> findDeleted(Pageable pageable);

    @Query(value = "select * from classifications where classification_id = :classificationId and is_deleted = true", nativeQuery = true)
    Optional<Classification> findDeletedById(@Param("classificationId") Long classificationId);

    Page<Classification> findByClassificationNameContainingIgnoreCaseOrClassificationSystemContainingIgnoreCase(
            String classificationName,
            String classificationSystem,
            Pageable pageable
    );

    @Modifying
    @Query(value = "update classifications set is_deleted = true where classification_id = :classificationId", nativeQuery = true)
    void softDeleteById(@Param("classificationId") Long classificationId);

    @Modifying
    @Query(value = "update classifications set is_deleted = false where classification_id = :classificationId", nativeQuery = true)
    void restoreById(@Param("classificationId") Long classificationId);
}
