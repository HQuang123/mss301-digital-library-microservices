package fu.edu.mss301.digilib.catalog.infrastructure.adapter;

import fu.edu.mss301.digilib.catalog.domain.entity.BookAuditLog;
import fu.edu.mss301.digilib.catalog.domain.repository.BookAuditLogRepository;
import fu.edu.mss301.digilib.catalog.infrastructure.persistence.BookAuditLogJpaRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class BookAuditLogRepositoryAdapter implements BookAuditLogRepository {

    private final BookAuditLogJpaRepository bookAuditLogJpaRepository;

    @Override
    public Page<BookAuditLog> findAllBookAuditLogs(Pageable pageable) {
        return bookAuditLogJpaRepository.findAll(pageable);
    }

    @Override
    public Optional<BookAuditLog> findBookAuditLogById(Integer logId) {
        return bookAuditLogJpaRepository.findById(logId);
    }

    @Override
    public Page<BookAuditLog> findBookAuditLogsByBookId(Long bookId, Pageable pageable) {
        return bookAuditLogJpaRepository.findByBookBookId(bookId, pageable);
    }

    @Override
    public Page<BookAuditLog> searchBookAuditLogs(String keyword, Pageable pageable) {
        if (keyword == null || keyword.isBlank()) {
            return Page.empty(pageable);
        }

        String normalizedKeyword = keyword.trim().toUpperCase();
        List<BookAuditLog.AuditAction> actions = Arrays.stream(BookAuditLog.AuditAction.values())
                .filter(action -> action.name().contains(normalizedKeyword))
                .toList();

        if (actions.isEmpty()) {
            return Page.empty(pageable);
        }

        return bookAuditLogJpaRepository.findByActionIn(actions, pageable);
    }

    @Override
    public Page<BookAuditLog> searchBookAuditLogs(
            Long bookId,
            BookAuditLog.AuditAction action,
            Integer userId,
            LocalDate changedFrom,
            LocalDate changedTo,
            Pageable pageable
    ) {
        LocalDateTime from = changedFrom != null ? changedFrom.atStartOfDay() : null;
        LocalDateTime to = changedTo != null ? changedTo.plusDays(1).atStartOfDay().minusNanos(1) : null;
        return bookAuditLogJpaRepository.findAll(byFilter(bookId, action, userId, from, to), pageable);
    }

    private Specification<BookAuditLog> byFilter(
            Long bookId,
            BookAuditLog.AuditAction action,
            Integer userId,
            LocalDateTime changedFrom,
            LocalDateTime changedTo
    ) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (bookId != null) {
                predicates.add(builder.equal(root.get("book").get("bookId"), bookId));
            }

            if (action != null) {
                predicates.add(builder.equal(root.get("action"), action));
            }

            if (userId != null) {
                predicates.add(builder.equal(root.get("userId"), userId));
            }

            if (changedFrom != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("changedAt"), changedFrom));
            }

            if (changedTo != null) {
                predicates.add(builder.lessThanOrEqualTo(root.get("changedAt"), changedTo));
            }

            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }
}
