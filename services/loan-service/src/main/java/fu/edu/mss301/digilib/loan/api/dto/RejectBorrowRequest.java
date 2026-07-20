package fu.edu.mss301.digilib.loan.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectBorrowRequest(
        @NotBlank @Size(max = 500) String reason
) {
}
