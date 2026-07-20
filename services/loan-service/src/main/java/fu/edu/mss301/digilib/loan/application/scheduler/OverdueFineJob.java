package fu.edu.mss301.digilib.loan.application.scheduler;

import fu.edu.mss301.digilib.loan.application.usecase.ManageLoanUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "loan.jobs.overdue-fine-enabled",
        havingValue = "true",
        matchIfMissing = true)
public class OverdueFineJob {

    private static final int LOST_THRESHOLD_DAYS = 30;

    private final ManageLoanUseCase manageLoanUseCase;

    @Scheduled(cron = "${loan.jobs.overdue-fine-cron:0 15 2 * * *}")
    public void run() {
        LocalDateTime now = LocalDateTime.now();
        for (Long loanId : manageLoanUseCase.findLoansOverdueByMoreThanDays(LOST_THRESHOLD_DAYS, now)) {
            try {
                manageLoanUseCase.createOrUpdateThresholdFine(loanId, now);
            } catch (RuntimeException exception) {
                log.error("Could not create or update threshold fine for loanId={}", loanId, exception);
            }
        }
    }
}
