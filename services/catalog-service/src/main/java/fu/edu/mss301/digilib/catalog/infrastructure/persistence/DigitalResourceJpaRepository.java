package fu.edu.mss301.digilib.catalog.infrastructure.persistence;

import fu.edu.mss301.digilib.catalog.domain.entity.DigitalResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface DigitalResourceJpaRepository extends JpaRepository<DigitalResource, Long>, JpaSpecificationExecutor<DigitalResource> {

    @Override
    @EntityGraph(attributePaths = {"book"})
    Page<DigitalResource> findAll(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"book"})
    Page<DigitalResource> findAll(Specification<DigitalResource> spec, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"book"})
    Optional<DigitalResource> findById(Long resourceId);

    @Query(value = """
            select * from digital_resources
            where resource_id = :resourceId
            """, nativeQuery = true)
    Optional<DigitalResource> findByIdIncludingDeleted(@Param("resourceId") Long resourceId);

    @Query(
            value = """
                    select
                        resource_id as resourceId,
                        file_format as fileFormat,
                        resource_url as resourceUrl,
                        access_permission as accessPermission,
                        uploaded_at as uploadedAt,
                        book_id as bookId,
                        is_deleted as isDeleted
                    from digital_resources
                    where is_deleted = true
                    """,
            countQuery = """
                    select count(*)
                    from digital_resources
                    where is_deleted = true
                    """,
            nativeQuery = true
    )
    Page<DigitalResourceRowProjection> findDeletedResourceRows(Pageable pageable);

    @EntityGraph(attributePaths = {"book"})
    List<DigitalResource> findByBookBookId(Long bookId);

    @Query(value = """
            select * from digital_resources
            where book_id = :bookId
            """, nativeQuery = true)
    List<DigitalResource> findByBookBookIdIncludingDeleted(@Param("bookId") Long bookId);

    @EntityGraph(attributePaths = {"book"})
    Page<DigitalResource> findByBookBookId(Long bookId, Pageable pageable);

    @EntityGraph(attributePaths = {"book"})
    Page<DigitalResource> findByFileFormatContainingIgnoreCaseOrResourceUrlContainingIgnoreCase(
            String fileFormat,
            String resourceUrl,
            Pageable pageable
    );

}
