package fu.edu.mss301.digilib.notification.infrastructure.persistence;

import fu.edu.mss301.digilib.notification.domain.entity.NotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDateTime;

public interface NotificationJpaRepository
        extends JpaRepository<NotificationLog, Integer>, JpaSpecificationExecutor<NotificationLog> {

    boolean existsByTemplate_EventTypeAndStudentIdAndCreateAtBetween(
            String eventType, String studentId, LocalDateTime rangeStart, LocalDateTime rangeEnd);
}
