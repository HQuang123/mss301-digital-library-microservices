package fu.edu.mss301.digilib.notification.infrastructure.persistence;

import fu.edu.mss301.digilib.notification.domain.entity.NotificationPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificationPolicyJpaRepository extends JpaRepository<NotificationPolicy, Integer> {

    Optional<NotificationPolicy> findByEventTypeAndIsActiveTrue(String eventType);
}
