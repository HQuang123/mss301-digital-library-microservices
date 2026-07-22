package fu.edu.mss301.digilib.fine.api.dto;

import fu.edu.mss301.digilib.fine.domain.entity.Fine;
import fu.edu.mss301.digilib.fine.domain.vo.FineReason;
import fu.edu.mss301.digilib.fine.domain.vo.FineStatus;

import java.time.LocalDateTime;

public record FineResponse(
        Integer fineId,
        Long loanId,
        Long bookId,
        String bookTitle,
        String studentId,
        String studentName,
        String studentEmail,
        FineReason reason,
        LocalDateTime dueDate,
        LocalDateTime returnDate,
        Long amountDue,
        Long compensationAmount,
        FineStatus status,
        LocalDateTime paidAt
) {
    public static FineResponse from(Fine fine) {
        return from(fine, null, null);
    }

    public static FineResponse from(Fine fine, String bookTitle) {
        return from(fine, bookTitle, null);
    }

    public static FineResponse from(Fine fine, String bookTitle, String studentName) {

        return new FineResponse(
                fine.getId(),
                fine.getLoanId(),
                fine.getBookId(),
                bookTitle,
                fine.getStudentId(),
                studentName,
                fine.getStudentEmail(),
                fine.getReason(),
                fine.getDueDate(),
                fine.getReturnDate(),
                fine.getAmountDue(),
                fine.getCompensationAmount(),
                fine.getStatus(),
                fine.getPaidAt()
        );
    }
}
