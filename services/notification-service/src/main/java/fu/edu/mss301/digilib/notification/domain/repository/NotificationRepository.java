package fu.edu.mss301.digilib.notification.domain.repository;

import fu.edu.mss301.digilib.notification.domain.entity.NotificationLog;
import fu.edu.mss301.digilib.notification.domain.entity.NotificationPolicy;
import fu.edu.mss301.digilib.notification.infrastructure.specification.NotificationSearchCriteria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Optional;

public interface NotificationRepository {

    Optional<NotificationPolicy> findActivePolicyByEventType(String eventType);

    NotificationLog save(NotificationLog log);

    Optional<NotificationLog> findById(Integer id);

    boolean existsByEventTypeAndStudentIdAndCreateAtBetween(
            String eventType, String studentId, LocalDateTime rangeStart, LocalDateTime rangeEnd);

    Page<NotificationLog> search(NotificationSearchCriteria criteria, Pageable pageable);
}
