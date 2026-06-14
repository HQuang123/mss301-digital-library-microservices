package fu.edu.mss301.digilib.catalog.domain.repository;

import fu.edu.mss301.digilib.catalog.domain.entity.BookAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.Optional;

public interface BookAuditLogRepository {

    Page<BookAuditLog> findAllBookAuditLogs(Pageable pageable);

    Optional<BookAuditLog> findBookAuditLogById(Integer logId);

    Page<BookAuditLog> findBookAuditLogsByBookId(Long bookId, Pageable pageable);

    Page<BookAuditLog> searchBookAuditLogs(String keyword, Pageable pageable);

    Page<BookAuditLog> searchBookAuditLogs(
            Long bookId,
            BookAuditLog.AuditAction action,
            Integer userId,
            LocalDate changedFrom,
            LocalDate changedTo,
            Pageable pageable
    );
}
