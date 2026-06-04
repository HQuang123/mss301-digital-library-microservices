package fu.edu.mss301.digilib.notification.domain.aggregate;

import fu.edu.mss301.digilib.notification.domain.entity.NotificationLog;
import fu.edu.mss301.digilib.notification.domain.entity.NotificationPolicy;
import fu.edu.mss301.digilib.notification.domain.vo.NotificationChannel;
import fu.edu.mss301.digilib.notification.domain.vo.NotificationStatus;

import java.time.LocalDateTime;
import java.util.Objects;

public final class NotificationAggregate {

    private final NotificationPolicy policy;

    private NotificationAggregate(NotificationPolicy policy) {
        this.policy = policy;
    }

    public static NotificationAggregate create(NotificationPolicy policy) {
        validatePolicy(policy);

        return new NotificationAggregate(policy);
    }

    public NotificationLog createLogFor(Integer studentId, String studentEmail) {
        return createEmailLogFor(studentId, studentEmail);
    }

    public NotificationLog createEmailLogFor(Integer studentId, String studentEmail) {
        validatePolicy(policy);
        Integer validStudentId = requirePositive(studentId, "studentId");
        String validStudentEmail = requireText(studentEmail, "studentEmail");

        return NotificationLog.builder()
                .template(policy)
                .studentId(validStudentId)
                .studentEmail(validStudentEmail)
                .channel(NotificationChannel.EMAIL)
                .status(NotificationStatus.PENDING)
                .build();
    }

    public NotificationLog createWebsiteLogFor(Integer studentId) {
        validatePolicy(policy);
        Integer validStudentId = requirePositive(studentId, "studentId");

        return NotificationLog.builder()
                .template(policy)
                .studentId(validStudentId)
                .channel(NotificationChannel.WEBSITE)
                .status(NotificationStatus.UNREAD)
                .build();
    }

    public void markSent(NotificationLog log) {
        NotificationLog validLog = requireEmailLog(log);

        validLog.setStatus(NotificationStatus.SENT);
        validLog.setSentAt(LocalDateTime.now());
        validLog.setFailureReason(null);
    }

    public void markFailed(NotificationLog log) {
        markFailed(log, null);
    }

    public void markFailed(NotificationLog log, String failureReason) {
        NotificationLog validLog = requireEmailLog(log);

        validLog.setStatus(NotificationStatus.FAILED);
        validLog.setFailureReason(normalizeNullableText(failureReason));
    }

    public void markRead(NotificationLog log) {
        NotificationLog validLog = requireWebsiteLog(log);

        validLog.setStatus(NotificationStatus.READ);
        validLog.setReadAt(LocalDateTime.now());
    }

    public void markUnread(NotificationLog log) {
        NotificationLog validLog = requireWebsiteLog(log);

        validLog.setStatus(NotificationStatus.UNREAD);
        validLog.setReadAt(null);
    }

    public Integer getId() {
        return policy.getId();
    }

    public String getEventType() {
        return policy.getEventType();
    }

    public String getSubjectTemplate() {
        return policy.getSubjectTemplate();
    }

    public String getBodyTemplate() {
        return policy.getBodyTemplate();
    }

    public boolean isActive() {
        return Boolean.TRUE.equals(policy.getIsActive());
    }

    private NotificationLog requireEmailLog(NotificationLog log) {
        NotificationLog validLog = requirePolicyLog(log);

        if (validLog.getChannel() != NotificationChannel.EMAIL) {
            throw new IllegalArgumentException("notificationLog must be an email notification");
        }

        return validLog;
    }

    private NotificationLog requireWebsiteLog(NotificationLog log) {
        NotificationLog validLog = requirePolicyLog(log);

        if (validLog.getChannel() != NotificationChannel.WEBSITE) {
            throw new IllegalArgumentException("notificationLog must be a website notification");
        }

        return validLog;
    }

    private NotificationLog requirePolicyLog(NotificationLog log) {
        if (log == null) {
            throw new IllegalArgumentException("notificationLog must not be null");
        }

        NotificationPolicy logPolicy = log.getTemplate();
        if (logPolicy == null) {
            throw new IllegalArgumentException("notificationLog policy must not be null");
        }

        if (!isSamePolicy(logPolicy)) {
            throw new IllegalArgumentException("notificationLog does not belong to this policy");
        }

        return log;
    }

    private boolean isSamePolicy(NotificationPolicy otherPolicy) {
        Integer policyId = policy.getId();
        Integer otherPolicyId = otherPolicy.getId();

        if (policyId != null && otherPolicyId != null) {
            return Objects.equals(policyId, otherPolicyId);
        }

        return policy == otherPolicy;
    }

    private static void validatePolicy(NotificationPolicy policy) {
        if (policy == null) {
            throw new IllegalArgumentException("notificationPolicy must not be null");
        }

        requireText(policy.getEventType(), "eventType");
        requireText(policy.getSubjectTemplate(), "subjectTemplate");
        requireText(policy.getBodyTemplate(), "bodyTemplate");

        if (!Boolean.TRUE.equals(policy.getIsActive())) {
            throw new IllegalStateException("notificationPolicy must be active");
        }
    }

    private static Integer requirePositive(Integer value, String fieldName) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }

        return value;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }

        return value.trim();
    }

    private static String normalizeNullableText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}
