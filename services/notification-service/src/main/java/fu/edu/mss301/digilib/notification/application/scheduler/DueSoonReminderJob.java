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
public class DueSoonReminderJob {

    private static final int DUE_SOON_DAYS = 3;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final LoanServiceClient loanServiceClient;
    private final CreateNewNotificationUseCase createNewNotificationUseCase;

    @Scheduled(cron = "${notification.jobs.due-soon-cron:0 0 7 * * *}")
    public void run() {
        log.info("Running due-soon reminder job for loans due in {} days", DUE_SOON_DAYS);

        for (LoanDueDto loan : loanServiceClient.getLoansDueInDays(DUE_SOON_DAYS)) {
            try {
                createNewNotificationUseCase.execute(NotificationCommand.builder()
                        .eventType(NotificationEventType.DUE_SOON.name())
                        .studentId(loan.studentId())
                        .studentEmail(loan.studentEmail())
                        .templateVariables(Map.of(
                                "bookTitle", loan.bookTitle(),
                                "dueDate", loan.dueDate().format(DATE_FORMAT)))
                        .build());
            } catch (Exception ex) {
                log.warn("Failed to send due-soon reminder for loanId={}", loan.loanId(), ex);
            }
        }
    }
}
