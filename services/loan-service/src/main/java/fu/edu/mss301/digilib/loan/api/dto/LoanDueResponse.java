package fu.edu.mss301.digilib.loan.api.dto;

import java.time.LocalDate;

public record LoanDueResponse(
        String loanId,
        String studentId,
        String studentEmail,
        String bookTitle,
        LocalDate dueDate
) {}
