package fu.edu.mss301.digilib.loan.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateBorrowRequest(
        @NotNull Long bookId,
        @NotBlank String bookType,
        @NotBlank String idempotencyKey
) {
}
