package fu.edu.mss301.digilib.fine.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Mirrors FineClientAdapter.LostBookFineRequest in loan-service —
 * field names must match for JSON deserialization.
 */
public record LostBookFineRequest(
        @NotBlank String studentId,
        @NotBlank String loanId,
        String bookId,
        String bookCopyId,
        String bookTitle,
        Long bookValue,
        @NotNull @PositiveOrZero Long overdueDays
) {
}
