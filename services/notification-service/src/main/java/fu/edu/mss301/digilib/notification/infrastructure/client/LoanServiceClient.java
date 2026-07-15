package fu.edu.mss301.digilib.notification.infrastructure.client;

import fu.edu.mss301.digilib.notification.infrastructure.client.dto.LoanDueDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * Loan-service does not yet expose these read endpoints (its controller layer is still a stub),
 * so the scheduled jobs calling this client will fail until loan-service implements them.
 */
@FeignClient(name = "loan-service")
public interface LoanServiceClient {

    @GetMapping("/api/loan/due-soon")
    List<LoanDueDto> getLoansDueInDays(@RequestParam("days") int days);

    @GetMapping("/api/loan/overdue")
    List<LoanDueDto> getOverdueLoans();
}
