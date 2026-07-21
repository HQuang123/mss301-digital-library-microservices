package fu.edu.mss301.digilib.loan.api.dto;

import fu.edu.mss301.digilib.loan.domain.entity.BorrowRequest;
import fu.edu.mss301.digilib.loan.domain.vo.BorrowRequestStatus;

import java.time.LocalDateTime;

public record BorrowRequestResponse(
        Long requestId,
        String memberId,
        Long bookId,
        String bookType,
        String status,
        Long loanId,
        LocalDateTime requestedAt,
        LocalDateTime processedAt,
        String processedBy,
        String rejectionReason
) {
    public static BorrowRequestResponse from(BorrowRequest request) {
        String effectiveStatus = request.getStatus().name();
        Long approvedLoanId = null;
        if (request.getStatus() == BorrowRequestStatus.APPROVED && request.getLoan() != null) {
            effectiveStatus = request.getLoan().getStatus().name();
            approvedLoanId = request.getLoan().getLoanId();
        }
        return new BorrowRequestResponse(
                request.getRequestId(),
                request.getMemberId(),
                request.getBookId(),
                request.getBookType(),
                effectiveStatus,
                approvedLoanId,
                request.getRequestedAt(),
                request.getProcessedAt(),
                request.getProcessedBy(),
                request.getRejectionReason());
    }
}
