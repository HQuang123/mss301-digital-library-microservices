package fu.edu.mss301.digilib.catalog.infrastructure.persistence;

import fu.edu.mss301.digilib.catalog.domain.entity.BookAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface BookAuditLogJpaRepository extends JpaRepository<BookAuditLog, Integer>, JpaSpecificationExecutor<BookAuditLog> {

    List<BookAuditLog> findByBookBookId(Long bookId);

    Page<BookAuditLog> findByBookBookId(Long bookId, Pageable pageable);

    Page<BookAuditLog> findByAction(BookAuditLog.AuditAction action, Pageable pageable);

    Page<BookAuditLog> findByActionIn(List<BookAuditLog.AuditAction> actions, Pageable pageable);
}
