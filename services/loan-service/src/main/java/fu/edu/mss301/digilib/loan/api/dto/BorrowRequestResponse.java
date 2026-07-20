package fu.edu.mss301.digilib.loan.api.dto;

import fu.edu.mss301.digilib.loan.domain.aggregate.Loan;
import fu.edu.mss301.digilib.loan.domain.vo.LoanStatus;

import java.time.LocalDateTime;

public record BorrowRequestResponse(
        Long requestId,
        String memberId,
        Long bookId,
        String bookType,
        LoanStatus status,
        LocalDateTime requestedAt,
        LocalDateTime reviewedAt,
        String reviewedBy,
        String rejectionReason,
        Long loanId
) {
    public static BorrowRequestResponse from(Loan loan) {
        return new BorrowRequestResponse(loan.getLoanId(), loan.getMemberId(), loan.getBookId(),
                loan.getBookType(), loan.getStatus(), loan.getCreatedAt(), loan.getReviewedAt(),
                loan.getReviewedBy(), loan.getRejectionReason(),
                loan.getStatus() == LoanStatus.BORROWED ? loan.getLoanId() : null);
    }
}
