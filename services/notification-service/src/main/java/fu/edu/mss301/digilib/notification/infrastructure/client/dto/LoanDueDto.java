package fu.edu.mss301.digilib.notification.infrastructure.client.dto;

import java.time.LocalDateTime;

public record LoanDueDto(
        Long loanId,
        String studentId,
        String studentEmail,
        String bookTitle,
        LocalDateTime dueDate) {
}
