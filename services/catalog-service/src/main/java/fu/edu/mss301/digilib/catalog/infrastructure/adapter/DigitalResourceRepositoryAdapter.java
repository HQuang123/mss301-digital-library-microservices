package fu.edu.mss301.digilib.catalog.infrastructure.adapter;

import fu.edu.mss301.digilib.catalog.domain.entity.DigitalResource;
import fu.edu.mss301.digilib.catalog.domain.repository.DigitalResourceRepository;
import fu.edu.mss301.digilib.catalog.infrastructure.persistence.DigitalResourceJpaRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class DigitalResourceRepositoryAdapter implements DigitalResourceRepository {

    private final DigitalResourceJpaRepository digitalResourceJpaRepository;

    @Override
    public DigitalResource saveDigitalResource(DigitalResource digitalResource) {
        return digitalResourceJpaRepository.save(digitalResource);
    }

    @Override
    public Page<DigitalResource> findAllDigitalResources(Pageable pageable) {
        return digitalResourceJpaRepository.findAll(pageable);
    }

    @Override
    public Optional<DigitalResource> findDigitalResourceById(Long resourceId) {
        return digitalResourceJpaRepository.findById(resourceId);
    }

    @Override
    public Page<DigitalResource> findDigitalResourcesByBookId(Long bookId, Pageable pageable) {
        return digitalResourceJpaRepository.findByBookBookId(bookId, pageable);
    }

    @Override
    public Page<DigitalResource> searchDigitalResources(String keyword, Pageable pageable) {
        return digitalResourceJpaRepository.findByFileFormatContainingIgnoreCaseOrResourceUrlContainingIgnoreCase(
                keyword,
                keyword,
                pageable
        );
    }

    @Override
    public Page<DigitalResource> filterDigitalResources(String fileFormat, String accessPermission, Pageable pageable) {
        return digitalResourceJpaRepository.findAll(byFilter(fileFormat, accessPermission), pageable);
    }

    @Override
    public void deleteDigitalResourceById(Long resourceId) {
        digitalResourceJpaRepository.deleteById(resourceId);
    }

    private Specification<DigitalResource> byFilter(String fileFormat, String accessPermission) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (fileFormat != null && !fileFormat.isBlank()) {
                predicates.add(builder.equal(root.get("fileFormat"), fileFormat));
            }

            if (accessPermission != null && !accessPermission.isBlank()) {
                predicates.add(builder.equal(root.get("accessPermission"), accessPermission));
            }

            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }
}
