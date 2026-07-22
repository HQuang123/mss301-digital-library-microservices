package fu.edu.mss301.digilib.loan.infrastructure.adapter;

import fu.edu.mss301.digilib.loan.application.exception.DownstreamServiceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.LocalDate;

@Component
public class FineClientAdapter {

    private final RestClient restClient;
    private final String internalApiKey;

    public FineClientAdapter(
            RestClient.Builder restClientBuilder,
            @Value("${services.fine.base-url}") String fineServiceBaseUrl,
            @Value("${services.internal-api-key}") String internalApiKey
    ) {
        this.internalApiKey = internalApiKey;
        this.restClient = restClientBuilder.baseUrl(fineServiceBaseUrl).build();
    }

    public void assertCanBorrow(String studentId) {
        try {
            BorrowEligibilityResponse response = restClient.get()
                    .uri("/internal/fines/students/{studentId}/borrow-eligibility", studentId)
                    .header("X-Internal-Api-Key", internalApiKey)
                    .retrieve()
                    .body(BorrowEligibilityResponse.class);

            if (response == null) {
                throw new DownstreamServiceException("Dịch vụ khoản phạt không trả về kết quả kiểm tra điều kiện mượn", null);
            }
            if (!response.canBorrow()) {
                String reason = response.reason() == null || response.reason().isBlank()
                        ? "Bạn đang có khoản phạt chưa thanh toán"
                        : response.reason();
                throw new IllegalStateException(reason);
            }
        } catch (IllegalStateException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new DownstreamServiceException("Không thể kiểm tra điều kiện mượn với dịch vụ khoản phạt", exception);
        }
    }

    public void createOverdueReturnFine(FineContext context, LocalDate returnDate, long overdueDays) {
        OverdueReturnFineRequest request = new OverdueReturnFineRequest(
                context.studentId(), context.loanId(), context.bookId(), context.bookCopyId(),
                context.bookTitle(), context.bookValue(), context.dueDate(), returnDate, overdueDays);
        post("/internal/fines/from-overdue-return", request);
    }

    public void createOverdueThresholdFine(FineContext context, long overdueDays) {
        OverdueThresholdFineRequest request = new OverdueThresholdFineRequest(
                context.studentId(), context.loanId(), context.bookId(), context.bookCopyId(),
                context.bookTitle(), context.bookValue(), overdueDays, true);
        post("/internal/fines/from-overdue-threshold", request);
    }

    public void createLostBookFine(FineContext context, long overdueDays) {
        LostBookFineRequest request = new LostBookFineRequest(
                context.studentId(), context.loanId(), context.bookId(), context.bookCopyId(),
                context.bookTitle(), context.bookValue(), overdueDays);
        post("/internal/fines/from-lost-book", request);
    }

    private void post(String path, Object request) {
        try {
            restClient.post()
                    .uri(path)
                    .header("X-Internal-Api-Key", internalApiKey)
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException exception) {
            throw new DownstreamServiceException("Không thể kết nối đến dịch vụ khoản phạt", exception);
        }
    }

    public record FineContext(
            String studentId,
            String loanId,
            String bookId,
            String bookCopyId,
            String bookTitle,
            long bookValue,
            LocalDate dueDate
    ) {}

    private record BorrowEligibilityResponse(boolean canBorrow, String reason) {}

    private record OverdueReturnFineRequest(
            String studentId,
            String loanId,
            String bookId,
            String bookCopyId,
            String bookTitle,
            long bookValue,
            LocalDate dueDate,
            LocalDate returnDate,
            long overdueDays
    ) {}

    private record OverdueThresholdFineRequest(
            String studentId,
            String loanId,
            String bookId,
            String bookCopyId,
            String bookTitle,
            long bookValue,
            long overdueDays,
            boolean compensationEnabled
    ) {}

    private record LostBookFineRequest(
            String studentId,
            String loanId,
            String bookId,
            String bookCopyId,
            String bookTitle,
            long bookValue,
            long overdueDays
    ) {}
}
