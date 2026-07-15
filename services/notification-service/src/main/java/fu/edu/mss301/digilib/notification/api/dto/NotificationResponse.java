package fu.edu.mss301.digilib.notification.api.dto;

import fu.edu.mss301.digilib.notification.domain.entity.NotificationLog;
import fu.edu.mss301.digilib.notification.domain.vo.NotificationChannel;
import fu.edu.mss301.digilib.notification.domain.vo.NotificationStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class NotificationResponse {

    private Integer id;
    private String eventType;
    private Integer studentId;
    private String studentEmail;
    private NotificationChannel channel;
    private NotificationStatus status;
    private LocalDateTime sentAt;
    private String failureReason;

    public static NotificationResponse from(NotificationLog log) {
        return NotificationResponse.builder()
                .id(log.getId())
                .eventType(log.getTemplate().getEventType())
                .studentId(log.getStudentId())
                .studentEmail(log.getStudentEmail())
                .channel(log.getChannel())
                .status(log.getStatus())
                .sentAt(log.getSentAt())
                .failureReason(log.getFailureReason())
                .build();
    }
}
