package fu.edu.mss301.digilib.catalog.api.dto;

import fu.edu.mss301.digilib.catalog.domain.entity.Book;
import fu.edu.mss301.digilib.catalog.domain.entity.BookAuditLog;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class BookAuditLogResponse {
    private Integer logId;
    private BookAuditLog.AuditAction action;
    private Integer userId;
    private LocalDateTime changedAt;
    private Long bookId;

    public static BookAuditLogResponse from(BookAuditLog log) {
        Book book = log.getBook();
        return new BookAuditLogResponse(
                log.getLogId(),
                log.getAction(),
                log.getUserId(),
                log.getChangedAt(),
                book != null ? book.getBookId() : null
        );
    }
}
