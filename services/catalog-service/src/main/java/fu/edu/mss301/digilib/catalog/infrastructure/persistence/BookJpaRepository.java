package fu.edu.mss301.digilib.catalog.infrastructure.persistence;

import fu.edu.mss301.digilib.catalog.domain.entity.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface BookJpaRepository extends JpaRepository<Book, Long>, JpaSpecificationExecutor<Book> {

    @Override
    @EntityGraph(attributePaths = {"category", "classification"})
    Page<Book> findAll(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"category", "classification"})
    Page<Book> findAll(Specification<Book> spec, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"category", "classification"})
    Optional<Book> findById(Long bookId);

    @Query(value = """
            select * from books
            where is_deleted = true
            """,
            countQuery = """
                    select count(*)
                    from books
                    where is_deleted = true
                    """,
            nativeQuery = true)
    Page<Book> findDeletedBooks(Pageable pageable);

    @Query(value = """
            select * from books
            where book_id = :bookId
            """, nativeQuery = true)
    Optional<Book> findByIdIncludingDeleted(@Param("bookId") Long bookId);

    @EntityGraph(attributePaths = {"category", "classification"})
    Optional<Book> findByIsbn(String isbn);

    @EntityGraph(attributePaths = {"category", "classification"})
    Page<Book> findByTitleContainingIgnoreCaseOrAuthorContainingIgnoreCaseOrIsbnContainingIgnoreCase(
            String title,
            String author,
            String isbn,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"category", "classification"})
    Page<Book> findByCategoryCategoryId(Long categoryId, Pageable pageable);

    @EntityGraph(attributePaths = {"category", "classification"})
    Page<Book> findByClassificationClassificationId(Long classificationId, Pageable pageable);

    @EntityGraph(attributePaths = {"category", "classification"})
    Page<Book> findByBookStatus(Book.BookStatus bookStatus, Pageable pageable);
}
