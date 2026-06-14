package fu.edu.mss301.digilib.catalog.infrastructure.persistence;

import fu.edu.mss301.digilib.catalog.domain.entity.BookCopy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface BookCopyJpaRepository extends JpaRepository<BookCopy, Long>, JpaSpecificationExecutor<BookCopy> {

    Optional<BookCopy> findByBarcode(String barcode);

    List<BookCopy> findByBookBookId(Long bookId);

    Page<BookCopy> findByBookBookId(Long bookId, Pageable pageable);

    Page<BookCopy> findByBarcodeContainingIgnoreCaseOrShelfLocationContainingIgnoreCase(
            String barcode,
            String shelfLocation,
            Pageable pageable
    );

    long countByCopyStatus(BookCopy.CopyStatus copyStatus);
}
