package fu.edu.mss301.digilib.catalog.infrastructure.persistence;

import fu.edu.mss301.digilib.catalog.domain.entity.DigitalResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface DigitalResourceJpaRepository extends JpaRepository<DigitalResource, Long>, JpaSpecificationExecutor<DigitalResource> {

    List<DigitalResource> findByBookBookId(Long bookId);

    Page<DigitalResource> findByBookBookId(Long bookId, Pageable pageable);

    Page<DigitalResource> findByFileFormatContainingIgnoreCaseOrResourceUrlContainingIgnoreCase(
            String fileFormat,
            String resourceUrl,
            Pageable pageable
    );
}
