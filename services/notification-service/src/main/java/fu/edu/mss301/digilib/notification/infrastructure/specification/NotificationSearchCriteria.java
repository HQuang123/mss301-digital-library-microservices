package fu.edu.mss301.digilib.notification.infrastructure.specification;

import fu.edu.mss301.digilib.notification.domain.entity.NotificationLog;
import fu.edu.mss301.digilib.notification.domain.vo.NotificationChannel;
import fu.edu.mss301.digilib.notification.domain.vo.NotificationStatus;
import jakarta.persistence.criteria.Predicate;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Builder
public class NotificationSearchCriteria {

    private String eventType;
    private Integer studentId;
    private NotificationChannel channel;
    private NotificationStatus status;
    private LocalDateTime createdFrom;
    private LocalDateTime createdTo;

    public Specification<NotificationLog> toSpecification() {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (eventType != null && !eventType.isBlank()) {
                predicates.add(cb.equal(root.get("template").get("eventType"), eventType));
            }
            if (studentId != null) {
                predicates.add(cb.equal(root.get("studentId"), studentId));
            }
            if (channel != null) {
                predicates.add(cb.equal(root.get("channel"), channel));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (createdFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createAt"), createdFrom));
            }
            if (createdTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createAt"), createdTo));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
