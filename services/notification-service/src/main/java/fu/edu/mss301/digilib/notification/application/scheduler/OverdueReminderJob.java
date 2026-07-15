package fu.edu.mss301.digilib.notification.application.scheduler;

import fu.edu.mss301.digilib.notification.application.command.NotificationCommand;
import fu.edu.mss301.digilib.notification.application.usecase.CreateNewNotificationUseCase;
import fu.edu.mss301.digilib.notification.domain.vo.NotificationEventType;
import fu.edu.mss301.digilib.notification.infrastructure.client.LoanServiceClient;
import fu.edu.mss301.digilib.notification.infrastructure.client.dto.LoanDueDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OverdueReminderJob {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final LoanServiceClient loanServiceClient;
    private final CreateNewNotificationUseCase createNewNotificationUseCase;

    /**
     * Runs daily; a fresh NotificationLog is created each run for every loan still overdue,
     * so students keep getting reminded until loan-service reports the book returned.
     */
    @Scheduled(cron = "${notification.jobs.overdue-cron:0 0 8 * * *}")
    public void run() {
        log.info("Running overdue reminder job");

        for (LoanDueDto loan : loanServiceClient.getOverdueLoans()) {
            try {
                createNewNotificationUseCase.execute(NotificationCommand.builder()
                        .eventType(NotificationEventType.OVERDUE_REMINDER.name())
                        .studentId(loan.studentId())
                        .studentEmail(loan.studentEmail())
                        .templateVariables(Map.of(
                                "bookTitle", loan.bookTitle(),
                                "dueDate", loan.dueDate().format(DATE_FORMAT)))
                        .build());
            } catch (Exception ex) {
                log.warn("Failed to send overdue reminder for loanId={}", loan.loanId(), ex);
            }
        }
    }
}
