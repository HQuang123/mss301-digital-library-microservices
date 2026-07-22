package fu.edu.mss301.digilib.loan.api.controller;

import fu.edu.mss301.digilib.loan.api.dto.LoanDueResponse;
import fu.edu.mss301.digilib.loan.application.usecase.ManageLoanUseCase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/loans/internal")
public class InternalLoanController {

    private final ManageLoanUseCase manageLoanUseCase;
    private final byte[] internalApiKey;

    public InternalLoanController(ManageLoanUseCase manageLoanUseCase,
                                   @Value("${services.internal-api-key}") String internalApiKey) {
        this.manageLoanUseCase = manageLoanUseCase;
        this.internalApiKey = internalApiKey.getBytes(StandardCharsets.UTF_8);
    }

    @GetMapping("/due-soon")
    public List<LoanDueResponse> dueSoon(@RequestParam(defaultValue = "3") int days,
                                         @RequestHeader("X-Internal-Api-Key") String apiKey) {
        verify(apiKey);
        if (days < 0 || days > 365) throw new IllegalArgumentException("Số ngày phải nằm trong khoảng từ 0 đến 365");
        return manageLoanUseCase.findDueSoon(days, LocalDateTime.now());
    }

    @GetMapping("/overdue")
    public List<LoanDueResponse> overdue(@RequestHeader("X-Internal-Api-Key") String apiKey) {
        verify(apiKey);
        return manageLoanUseCase.findOverdue(LocalDateTime.now());
    }

    /**
     * Constant-time comparison of the supplied key against the configured
     * value, mirroring member-service's {@code InternalApiKeyWebFilter} so
     * both services reject timing-attack attempts consistently.
     */
    private void verify(String value) {
        byte[] supplied = value == null ? new byte[0] : value.getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(internalApiKey, supplied)) {
            throw new IllegalStateException("Khóa API nội bộ không hợp lệ");
        }
    }
}
