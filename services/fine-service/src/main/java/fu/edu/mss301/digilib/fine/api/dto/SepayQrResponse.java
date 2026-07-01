package fu.edu.mss301.digilib.fine.api.dto;

import fu.edu.mss301.digilib.fine.domain.vo.PaymentStatus;

import java.time.LocalDateTime;

public record SepayQrResponse(
        Long paymentId,
        Integer fineId,
        String paymentCode,
        Long amount,
        String currency,
        PaymentStatus status,
        String bank,
        String accountNumber,
        String accountName,
        String transferContent,
        String qrUrl,
        LocalDateTime expiresAt
) {
}
