package fu.edu.mss301.digilib.catalog.infrastructure.persistence;

import fu.edu.mss301.digilib.catalog.domain.entity.BookCopy;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BookCopyJpaRepository extends JpaRepository<BookCopy, Long>, JpaSpecificationExecutor<BookCopy> {

    @Query(
            value = "select * from book_copies where is_deleted = true",
            countQuery = "select count(*) from book_copies where is_deleted = true",
            nativeQuery = true
    )
    Page<BookCopy> findDeleted(Pageable pageable);

    @Query(value = "select * from book_copies where copy_id = :copyId and is_deleted = true", nativeQuery = true)
    Optional<BookCopy> findDeletedById(@Param("copyId") Long copyId);

    Optional<BookCopy> findByBarcode(String barcode);

    List<BookCopy> findByBookBookId(Long bookId);

    Page<BookCopy> findByBookBookId(Long bookId, Pageable pageable);

    Page<BookCopy> findByBarcodeContainingIgnoreCaseOrShelfLocationContainingIgnoreCase(
            String barcode,
            String shelfLocation,
            Pageable pageable
    );

    long countByCopyStatus(BookCopy.CopyStatus copyStatus);

    @Modifying
    @Query(value = "update book_copies set is_deleted = false where copy_id = :copyId", nativeQuery = true)
    void restoreById(@Param("copyId") Long copyId);
}
