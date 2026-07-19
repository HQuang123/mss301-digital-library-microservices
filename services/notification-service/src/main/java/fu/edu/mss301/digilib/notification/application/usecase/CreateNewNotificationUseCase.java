package fu.edu.mss301.digilib.notification.application.usecase;

import fu.edu.mss301.digilib.notification.application.command.NotificationCommand;
import fu.edu.mss301.digilib.notification.application.service.MailSender;
import fu.edu.mss301.digilib.notification.application.service.TemplateRenderer;
import fu.edu.mss301.digilib.notification.domain.aggregate.NotificationAggregate;
import fu.edu.mss301.digilib.notification.domain.entity.NotificationLog;
import fu.edu.mss301.digilib.notification.domain.entity.NotificationPolicy;
import fu.edu.mss301.digilib.notification.domain.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreateNewNotificationUseCase {

    private final NotificationRepository notificationRepository;
    private final MailSender mailSender;

    /**
     * Creates and sends the email + in-app notification for a policy event.
     * Skips (returns empty) if a notification for the same event/student was already created today,
     * so daily jobs (due-soon, overdue) can run repeatedly without duplicating sends.
     */
    @Transactional
    public List<NotificationLog> execute(NotificationCommand command) {
        NotificationPolicy policy = notificationRepository.findActivePolicyByEventType(command.getEventType())
                .orElseThrow(() -> new IllegalStateException(
                        "No active notification policy for eventType=" + command.getEventType()));

        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);
        boolean alreadySentToday = notificationRepository.existsByEventTypeAndStudentIdAndCreateAtBetween(
                command.getEventType(), command.getStudentId(), startOfDay, endOfDay);

        if (alreadySentToday) {
            log.info("Skipping duplicate notification eventType={} studentId={} for today",
                    command.getEventType(), command.getStudentId());
            return List.of();
        }

        NotificationAggregate aggregate = NotificationAggregate.create(policy);

        String subject = TemplateRenderer.render(aggregate.getSubjectTemplate(), command.getTemplateVariables());
        String body = TemplateRenderer.render(aggregate.getBodyTemplate(), command.getTemplateVariables());

        NotificationLog emailLog = aggregate.createEmailLogFor(
                command.getStudentId(), command.getStudentEmail(), subject, body);
        NotificationLog websiteLog = aggregate.createWebsiteLogFor(command.getStudentId(), subject, body);

        emailLog = notificationRepository.save(emailLog);
        websiteLog = notificationRepository.save(websiteLog);

        sendEmail(aggregate, emailLog, subject, body);

        return List.of(emailLog, websiteLog);
    }

    private void sendEmail(NotificationAggregate aggregate, NotificationLog emailLog, String subject, String body) {
        try {
            mailSender.send(emailLog.getStudentEmail(), subject, body);
            aggregate.markSent(emailLog);
        } catch (Exception ex) {
            log.warn("Failed to send notification email to {}", emailLog.getStudentEmail(), ex);
            aggregate.markFailed(emailLog, ex.getMessage());
        }

        notificationRepository.save(emailLog);
    }
}
