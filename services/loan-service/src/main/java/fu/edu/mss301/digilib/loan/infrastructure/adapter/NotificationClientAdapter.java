package fu.edu.mss301.digilib.loan.infrastructure.adapter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.LocalDateTime;

@Slf4j
@Component
public class NotificationClientAdapter {

    private final RestClient restClient;
    private final String internalApiKey;

    public NotificationClientAdapter(
            RestClient.Builder restClientBuilder,
            @Value("${services.notification.base-url}") String notificationServiceBaseUrl,
            @Value("${services.internal-api-key}") String internalApiKey
    ) {
        this.internalApiKey = internalApiKey;
        this.restClient = restClientBuilder.baseUrl(notificationServiceBaseUrl).build();
    }

    public void sendReturnConfirmation(
            String memberId,
            String memberEmail,
            String bookTitle,
            LocalDateTime returnedAt
    ) {
        try {
            restClient.post()
                    .uri("/api/notifications/return-confirmation")
                    .header("X-Internal-Api-Key", internalApiKey)
                    .body(new ReturnConfirmationRequest(
                            legacyStudentId(memberId), memberEmail, bookTitle, returnedAt))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException exception) {
            // Notification delivery must not roll back a completed loan transaction.
            log.warn("Could not send return confirmation for memberId={}", memberId, exception);
        }
    }

    private int legacyStudentId(String memberId) {
        try {
            int value = Integer.parseInt(memberId);
            if (value > 0) {
                return value;
            }
        } catch (NumberFormatException ignored) {
            // Notification Service currently uses an Integer studentId while Member Service uses String IDs.
        }
        return Math.floorMod(memberId.hashCode(), Integer.MAX_VALUE - 1) + 1;
    }

    private record ReturnConfirmationRequest(
            Integer studentId,
            String studentEmail,
            String bookTitle,
            LocalDateTime returnedAt
    ) {}
}
