package fu.edu.mss301.digilib.loan.infrastructure.adapter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.LocalDateTime;
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

    public void sendReturnConfirmation(
            Long loanId,
            String memberId,
            String memberEmail,
            String bookTitle,
            LocalDateTime returnedAt
    ) {
        try {
            restClient.post()
                    .uri("/api/notifications/return-confirmation")
                    .header("X-Internal-Api-Key", internalApiKey)
                    .body(new ReturnConfirmationRequest(memberId, memberEmail, bookTitle, returnedAt,
                            "loan:" + loanId + ":returned"))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException exception) {
            // Notification delivery must not roll back a completed loan transaction.
            log.warn("Could not send return confirmation for memberId={}", memberId, exception);
        }
    }

    public void sendBorrowConfirmation(
            Long loanId, String memberId, String memberEmail, String bookTitle, LocalDateTime dueDate) {
        send("BOOK_BORROWED", memberId, memberEmail,
                Map.of("bookTitle", bookTitle, "dueDate", dueDate.toLocalDate().toString()),
                "loan:" + loanId + ":borrowed");
    }

    private void send(String eventType, String memberId, String memberEmail,
                      Map<String, String> variables, String correlationKey) {
        try {
            restClient.post()
                    .uri("/api/notifications")
                    .header("X-Internal-Api-Key", internalApiKey)
                    .body(new NotificationCreateRequest(
                            eventType, memberId, memberEmail, variables, correlationKey))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException exception) {
            log.warn("Could not send {} notification for memberId={}", eventType, memberId, exception);
        }
    }

    private record ReturnConfirmationRequest(
            String studentId,
            String studentEmail,
            String bookTitle,
            LocalDateTime returnedAt
            , String correlationKey
    ) {}

    private record NotificationCreateRequest(
            String eventType,
            String studentId,
            String studentEmail,
            Map<String, String> templateVariables,
            String correlationKey
    ) {}
}
