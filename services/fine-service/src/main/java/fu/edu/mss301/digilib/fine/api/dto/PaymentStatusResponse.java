package fu.edu.mss301.digilib.fine.api.dto;

import fu.edu.mss301.digilib.fine.domain.vo.FineStatus;
import fu.edu.mss301.digilib.fine.domain.vo.PaymentStatus;

import java.time.LocalDateTime;

public record PaymentStatusResponse(
        Long paymentId,
        Integer fineId,
        String paymentCode,
        Long amount,
        PaymentStatus paymentStatus,
        FineStatus fineStatus,
        LocalDateTime paidAt,
        LocalDateTime expiresAt
) {
}
