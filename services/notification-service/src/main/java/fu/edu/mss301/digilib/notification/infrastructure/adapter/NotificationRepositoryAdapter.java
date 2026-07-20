package fu.edu.mss301.digilib.notification.infrastructure.adapter;

import fu.edu.mss301.digilib.notification.domain.entity.NotificationLog;
import fu.edu.mss301.digilib.notification.domain.entity.NotificationPolicy;
import fu.edu.mss301.digilib.notification.domain.repository.NotificationRepository;
import fu.edu.mss301.digilib.notification.infrastructure.persistence.NotificationJpaRepository;
import fu.edu.mss301.digilib.notification.infrastructure.persistence.NotificationPolicyJpaRepository;
import fu.edu.mss301.digilib.notification.infrastructure.specification.NotificationSearchCriteria;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class NotificationRepositoryAdapter implements NotificationRepository {

    private final NotificationJpaRepository notificationJpaRepository;
    private final NotificationPolicyJpaRepository notificationPolicyJpaRepository;

    @Override
    public Optional<NotificationPolicy> findActivePolicyByEventType(String eventType) {
        return notificationPolicyJpaRepository.findByEventTypeAndIsActiveTrue(eventType);
    }

    @Override
    public NotificationLog save(NotificationLog log) {
        return notificationJpaRepository.save(log);
    }

    @Override
    public Optional<NotificationLog> findById(Integer id) {
        return notificationJpaRepository.findById(id);
    }

    @Override
    public boolean existsByEventTypeAndStudentIdAndCreateAtBetween(
            String eventType, String studentId, LocalDateTime rangeStart, LocalDateTime rangeEnd) {
        return notificationJpaRepository.existsByTemplate_EventTypeAndStudentIdAndCreateAtBetween(
                eventType, studentId, rangeStart, rangeEnd);
    }

    @Override
    public Page<NotificationLog> search(NotificationSearchCriteria criteria, Pageable pageable) {
        return notificationJpaRepository.findAll(criteria.toSpecification(), pageable);
    }
}
