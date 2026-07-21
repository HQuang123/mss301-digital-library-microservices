package fu.edu.mss301.digilib.fine.api.dto;

import fu.edu.mss301.digilib.fine.domain.entity.PaymentAttempt;
import fu.edu.mss301.digilib.fine.domain.vo.PaymentProvider;
import fu.edu.mss301.digilib.fine.domain.vo.PaymentStatus;

import java.time.LocalDateTime;

public record PaymentAttemptResponse(
        Long id,
        Integer fineId,
        String paymentCode,
        Long amount,
        String currency,
        PaymentProvider provider,
        PaymentStatus status,
        String sepayTransactionId,
        String sepayReferenceCode,
        LocalDateTime paidAt,
        LocalDateTime expiresAt,
        LocalDateTime createdAt
) {
    public static PaymentAttemptResponse from(PaymentAttempt attempt) {
        return new PaymentAttemptResponse(
                attempt.getId(),
                attempt.getFine().getId(),
                attempt.getPaymentCode(),
                attempt.getAmount(),
                attempt.getCurrency(),
                attempt.getProvider(),
                attempt.getStatus(),
                attempt.getSepayTransactionId(),
                attempt.getSepayReferenceCode(),
                attempt.getPaidAt(),
                attempt.getExpiresAt(),
                attempt.getCreatedAt()
        );
    }
}
