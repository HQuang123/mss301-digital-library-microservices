package fu.edu.mss301.digilib.notification.application.usecase;

import fu.edu.mss301.digilib.notification.domain.aggregate.NotificationAggregate;
import fu.edu.mss301.digilib.notification.domain.entity.NotificationLog;
import fu.edu.mss301.digilib.notification.domain.repository.NotificationRepository;
import fu.edu.mss301.digilib.notification.domain.vo.NotificationChannel;
import fu.edu.mss301.digilib.notification.domain.vo.NotificationStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class MarkNotificationReadUseCase {

    private final NotificationRepository notificationRepository;

    @Transactional
    public NotificationLog execute(Integer logId, String requestingStudentId) {
        NotificationLog log = notificationRepository.findById(logId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));

        if (!log.getStudentId().equals(requestingStudentId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized to mark this notification");
        }

        if (log.getChannel() != NotificationChannel.WEBSITE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only in-app (WEBSITE) notifications can be marked as read");
        }

        if (log.getStatus() == NotificationStatus.READ) {
            return log;
        }

        NotificationAggregate aggregate = NotificationAggregate.create(log.getTemplate());
        aggregate.markRead(log);
        return notificationRepository.save(log);
    }
}
