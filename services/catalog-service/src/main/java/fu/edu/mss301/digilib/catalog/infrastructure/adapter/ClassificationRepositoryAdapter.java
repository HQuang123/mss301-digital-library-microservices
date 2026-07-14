package fu.edu.mss301.digilib.catalog.infrastructure.adapter;

import fu.edu.mss301.digilib.catalog.domain.entity.Classification;
import fu.edu.mss301.digilib.catalog.domain.repository.ClassificationRepository;
import fu.edu.mss301.digilib.catalog.infrastructure.persistence.ClassificationJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ClassificationRepositoryAdapter implements ClassificationRepository {

    private final ClassificationJpaRepository classificationJpaRepository;

    @Override
    public Classification saveClassification(Classification classification) {
        return classificationJpaRepository.save(classification);
    }

    @Override
    public Page<Classification> findAllClassifications(Pageable pageable) {
        return classificationJpaRepository.findAll(pageable);
    }

    @Override
    public Page<Classification> findDeletedClassifications(Pageable pageable) {
        return classificationJpaRepository.findDeleted(pageable);
    }

    @Override
    public Optional<Classification> findClassificationById(Long classificationId) {
        return classificationJpaRepository.findById(classificationId);
    }

    @Override
    public Optional<Classification> findDeletedClassificationById(Long classificationId) {
        return classificationJpaRepository.findDeletedById(classificationId);
    }

    @Override
    public Page<Classification> searchClassifications(String keyword, Pageable pageable) {
        return classificationJpaRepository.findByClassificationNameContainingIgnoreCaseOrClassificationSystemContainingIgnoreCase(
                keyword,
                keyword,
                pageable
        );
    }

    @Override
    public void deleteClassificationById(Long classificationId) {
        classificationJpaRepository.softDeleteById(classificationId);
    }

    @Override
    public void restoreClassificationById(Long classificationId) {
        classificationJpaRepository.restoreById(classificationId);
    }
}
