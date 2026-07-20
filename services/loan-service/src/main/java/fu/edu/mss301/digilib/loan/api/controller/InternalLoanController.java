package fu.edu.mss301.digilib.loan.api.controller;

import fu.edu.mss301.digilib.loan.api.dto.LoanDueResponse;
import fu.edu.mss301.digilib.loan.application.usecase.ManageLoanUseCase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/loans/internal")
public class InternalLoanController {

    private final ManageLoanUseCase manageLoanUseCase;
    private final String internalApiKey;

    public InternalLoanController(ManageLoanUseCase manageLoanUseCase,
                                  @Value("${services.internal-api-key}") String internalApiKey) {
        this.manageLoanUseCase = manageLoanUseCase;
        this.internalApiKey = internalApiKey;
    }

    @GetMapping("/due-soon")
    public List<LoanDueResponse> dueSoon(@RequestParam(defaultValue = "3") int days,
                                        @RequestHeader("X-Internal-Api-Key") String apiKey) {
        verify(apiKey);
        if (days < 0 || days > 365) throw new IllegalArgumentException("days must be between 0 and 365");
        return manageLoanUseCase.findDueSoon(days, LocalDateTime.now());
    }

    @GetMapping("/overdue")
    public List<LoanDueResponse> overdue(@RequestHeader("X-Internal-Api-Key") String apiKey) {
        verify(apiKey);
        return manageLoanUseCase.findOverdue(LocalDateTime.now());
    }

    private void verify(String value) {
        if (!internalApiKey.equals(value)) throw new IllegalStateException("Invalid internal API key");
    }
}
