package fu.edu.mss301.digilib.catalog.application.command;

import fu.edu.mss301.digilib.catalog.domain.entity.BookAuditLog;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class BookAuditLogCommand {

    private Long bookId;
    private BookAuditLog.AuditAction action;
    private Integer userId;
    private LocalDate changedFrom;
    private LocalDate changedTo;
    private String keyword;
}
