package fu.edu.mss301.digilib.catalog.domain.repository;

import fu.edu.mss301.digilib.catalog.domain.entity.Classification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface ClassificationRepository {

    Classification saveClassification(Classification classification);

    Page<Classification> findAllClassifications(Pageable pageable);

    Page<Classification> findDeletedClassifications(Pageable pageable);

    Optional<Classification> findClassificationById(Long classificationId);

    Optional<Classification> findDeletedClassificationById(Long classificationId);

    Page<Classification> searchClassifications(String keyword, Pageable pageable);

    void deleteClassificationById(Long classificationId);

    void restoreClassificationById(Long classificationId);
}
