package fu.edu.mss301.digilib.fine.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.LocalDate;

/**
 * Mirrors FineClientAdapter.OverdueReturnFineRequest in loan-service —
 * field names must match for JSON deserialization.
 */
public record OverdueReturnFineRequest(
        @NotBlank String studentId,
        @NotBlank String loanId,
        String bookId,
        String bookCopyId,
        String bookTitle,
        Long bookValue,
        @NotNull LocalDate dueDate,
        @NotNull LocalDate returnDate,
        @NotNull @PositiveOrZero Long overdueDays
) {
}
