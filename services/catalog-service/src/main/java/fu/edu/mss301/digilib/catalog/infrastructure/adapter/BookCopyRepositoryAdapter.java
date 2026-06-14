package fu.edu.mss301.digilib.catalog.infrastructure.adapter;

import fu.edu.mss301.digilib.catalog.domain.entity.BookCopy;
import fu.edu.mss301.digilib.catalog.domain.repository.BookCopyRepository;
import fu.edu.mss301.digilib.catalog.infrastructure.persistence.BookCopyJpaRepository;
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
public class BookCopyRepositoryAdapter implements BookCopyRepository {

    private final BookCopyJpaRepository bookCopyJpaRepository;

    @Override
    public BookCopy saveBookCopy(BookCopy bookCopy) {
        return bookCopyJpaRepository.save(bookCopy);
    }

    @Override
    public Page<BookCopy> findAllBookCopies(Pageable pageable) {
        return bookCopyJpaRepository.findAll(pageable);
    }

    @Override
    public Optional<BookCopy> findBookCopyById(Long copyId) {
        return bookCopyJpaRepository.findById(copyId);
    }

    @Override
    public Optional<BookCopy> findBookCopyByBarcode(String barcode) {
        return bookCopyJpaRepository.findByBarcode(barcode);
    }

    @Override
    public Page<BookCopy> findBookCopiesByBookId(Long bookId, Pageable pageable) {
        return bookCopyJpaRepository.findByBookBookId(bookId, pageable);
    }

    @Override
    public Page<BookCopy> searchBookCopies(String keyword, Pageable pageable) {
        return bookCopyJpaRepository.findByBarcodeContainingIgnoreCaseOrShelfLocationContainingIgnoreCase(
                keyword,
                keyword,
                pageable
        );
    }

    @Override
    public Page<BookCopy> filterBookCopies(BookCopy.CopyStatus copyStatus, String shelfLocation, Pageable pageable) {
        return bookCopyJpaRepository.findAll(byFilter(copyStatus, shelfLocation), pageable);
    }

    @Override
    public void deleteBookCopyById(Long copyId) {
        bookCopyJpaRepository.deleteById(copyId);
    }

    private Specification<BookCopy> byFilter(BookCopy.CopyStatus copyStatus, String shelfLocation) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (copyStatus != null) {
                predicates.add(builder.equal(root.get("copyStatus"), copyStatus));
            }

            if (shelfLocation != null && !shelfLocation.isBlank()) {
                predicates.add(builder.equal(root.get("shelfLocation"), shelfLocation));
            }

            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }
}
