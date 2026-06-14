package fu.edu.mss301.digilib.catalog.api.controller;

import fu.edu.mss301.digilib.catalog.api.dto.BookAuditLogResponse;
import fu.edu.mss301.digilib.catalog.application.command.BookAuditLogCommand;
import fu.edu.mss301.digilib.catalog.application.usecase.ViewBookAuditLogUseCase;
import fu.edu.mss301.digilib.catalog.domain.entity.BookAuditLog;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class BookAuditLogController {

    private final ViewBookAuditLogUseCase viewBookAuditLogUseCase;

    @GetMapping("/book-audit-logs")
    public Page<BookAuditLogResponse> getBookAuditLogs(Pageable pageable) {
        return viewBookAuditLogUseCase.findAll(pageable)
                .map(BookAuditLogResponse::from);
    }

    @GetMapping("/book-audit-logs/{logId}")
    public BookAuditLogResponse getBookAuditLog(@PathVariable Integer logId) {
        return BookAuditLogResponse.from(viewBookAuditLogUseCase.findById(logId));
    }

    @GetMapping("/book-audit-logs/search")
    public Page<BookAuditLogResponse> searchBookAuditLogs(@RequestParam String keyword, Pageable pageable) {
        return viewBookAuditLogUseCase.search(
                        new BookAuditLogCommand(null, null, null, null, null, keyword),
                        pageable
                )
                .map(BookAuditLogResponse::from);
    }

    @GetMapping("/book-audit-logs/filter")
    public Page<BookAuditLogResponse> filterBookAuditLogs(
            @RequestParam(required = false) Long bookId,
            @RequestParam(required = false) BookAuditLog.AuditAction action,
            @RequestParam(required = false) Integer userId,
            @RequestParam(required = false) LocalDate changedFrom,
            @RequestParam(required = false) LocalDate changedTo,
            Pageable pageable) {
        return viewBookAuditLogUseCase.filter(
                        new BookAuditLogCommand(bookId, action, userId, changedFrom, changedTo, null),
                        pageable
                )
                .map(BookAuditLogResponse::from);
    }
}
