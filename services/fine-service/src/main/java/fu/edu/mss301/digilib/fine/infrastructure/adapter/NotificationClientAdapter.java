package fu.edu.mss301.digilib.fine.infrastructure.adapter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;

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

    public void sendFinePaidConfirmation(
            Integer fineId,
            String studentId,
            String studentEmail,
            Long amount,
            String paidAt
    ) {
        if (studentEmail == null || studentEmail.isBlank()) {
            log.warn("Skipping FINE_PAID notification for fineId={}: no student email on file", fineId);
            return;
        }

        try {
            restClient.post()
                    .uri("/api/notifications")
                    .header("X-Internal-Api-Key", internalApiKey)
                    .body(new NotificationCreateRequest(
                            "FINE_PAID",
                            studentId,
                            studentEmail,
                            Map.of(
                                    "fineId", String.valueOf(fineId),
                                    "amount", String.valueOf(amount),
                                    "paidAt", paidAt)))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException exception) {
            // Notification delivery must not roll back a completed payment.
            log.warn("Could not send FINE_PAID notification for fineId={}", fineId, exception);
        }
    }

    private record NotificationCreateRequest(
            String eventType,
            String studentId,
            String studentEmail,
            Map<String, String> templateVariables
    ) {}
}
