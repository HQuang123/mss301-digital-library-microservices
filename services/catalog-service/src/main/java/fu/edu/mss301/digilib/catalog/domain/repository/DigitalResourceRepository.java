package fu.edu.mss301.digilib.catalog.domain.repository;

import fu.edu.mss301.digilib.catalog.domain.entity.DigitalResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface DigitalResourceRepository {

    DigitalResource saveDigitalResource(DigitalResource digitalResource);

    Page<DigitalResource> findAllDigitalResources(Pageable pageable);

    Optional<DigitalResource> findDigitalResourceById(Long resourceId);

    Page<DigitalResource> findDigitalResourcesByBookId(Long bookId, Pageable pageable);

    Page<DigitalResource> searchDigitalResources(String keyword, Pageable pageable);

    Page<DigitalResource> filterDigitalResources(String fileFormat, String accessPermission, Pageable pageable);

    void deleteDigitalResourceById(Long resourceId);
}
