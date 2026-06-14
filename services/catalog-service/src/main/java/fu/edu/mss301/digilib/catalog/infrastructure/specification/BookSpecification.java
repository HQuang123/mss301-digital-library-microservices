package fu.edu.mss301.digilib.catalog.infrastructure.specification;

import fu.edu.mss301.digilib.catalog.domain.entity.Book;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class BookSpecification {

    private BookSpecification() {
    }

    public static Specification<Book> byKeyword(String keyword) {
        return (root, query, builder) -> {
            if (keyword == null || keyword.isBlank()) {
                return builder.conjunction();
            }

            String pattern = "%" + keyword.trim().toLowerCase() + "%";
            return builder.or(
                    builder.like(builder.lower(root.get("title")), pattern),
                    builder.like(builder.lower(root.get("author")), pattern),
                    builder.like(builder.lower(root.get("isbn")), pattern)
            );
        };
    }

    public static Specification<Book> byFilter(
            Long categoryId,
            Long classificationId,
            Book.BookStatus bookStatus
    ) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (categoryId != null) {
                predicates.add(builder.equal(root.get("category").get("categoryId"), categoryId));
            }

            if (classificationId != null) {
                predicates.add(builder.equal(root.get("classification").get("classificationId"), classificationId));
            }

            if (bookStatus != null) {
                predicates.add(builder.equal(root.get("bookStatus"), bookStatus));
            }

            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }
}
