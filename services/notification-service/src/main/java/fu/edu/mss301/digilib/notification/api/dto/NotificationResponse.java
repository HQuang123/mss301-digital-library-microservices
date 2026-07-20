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
    private String studentId;
    private String studentEmail;
    private NotificationChannel channel;
    private NotificationStatus status;
    private String subject;
    private String body;
    private LocalDateTime createdAt;
    private LocalDateTime sentAt;
    private LocalDateTime readAt;
    private String failureReason;

    public static NotificationResponse from(NotificationLog log) {
        return NotificationResponse.builder()
                .id(log.getId())
                .eventType(log.getTemplate().getEventType())
                .studentId(log.getStudentId())
                .studentEmail(log.getStudentEmail())
                .channel(log.getChannel())
                .status(log.getStatus())
                .subject(log.getTitle())
                .body(log.getMessage())
                .createdAt(log.getCreateAt())
                .sentAt(log.getSentAt())
                .readAt(log.getReadAt())
                .failureReason(log.getFailureReason())
                .build();
    }
}
