package fu.edu.mss301.digilib.catalog.infrastructure.persistence;

import fu.edu.mss301.digilib.catalog.domain.entity.Classification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClassificationJpaRepository extends JpaRepository<Classification, Long> {

    Page<Classification> findByClassificationNameContainingIgnoreCaseOrClassificationSystemContainingIgnoreCase(
            String classificationName,
            String classificationSystem,
            Pageable pageable
    );
}
