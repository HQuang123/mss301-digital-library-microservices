package fu.edu.mss301.digilib.catalog.application.usecase;

import fu.edu.mss301.digilib.catalog.application.command.BookAuditLogCommand;
import fu.edu.mss301.digilib.catalog.domain.entity.BookAuditLog;
import fu.edu.mss301.digilib.catalog.domain.repository.BookAuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ViewBookAuditLogUseCase {

    private final BookAuditLogRepository bookAuditLogRepository;

    public Page<BookAuditLog> findAll(Pageable pageable) {
        return bookAuditLogRepository.findAllBookAuditLogs(pageable);
    }

    public BookAuditLog findById(Integer logId) {
        return bookAuditLogRepository.findBookAuditLogById(logId)
                .orElseThrow(() -> new IllegalArgumentException("Book audit log not found"));
    }

    public Page<BookAuditLog> findByBook(Long bookId, Pageable pageable) {
        return bookAuditLogRepository.findBookAuditLogsByBookId(bookId, pageable);
    }

    public Page<BookAuditLog> search(BookAuditLogCommand command, Pageable pageable) {
        return bookAuditLogRepository.searchBookAuditLogs(command.getKeyword(), pageable);
    }

    public Page<BookAuditLog> filter(BookAuditLogCommand command, Pageable pageable) {
        return bookAuditLogRepository.searchBookAuditLogs(
                command.getBookId(),
                command.getAction(),
                command.getUserId(),
                command.getChangedFrom(),
                command.getChangedTo(),
                pageable
        );
    }
}
