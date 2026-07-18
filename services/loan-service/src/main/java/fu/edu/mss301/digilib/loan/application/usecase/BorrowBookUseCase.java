package fu.edu.mss301.digilib.loan.application.usecase;

import fu.edu.mss301.digilib.loan.application.command.BorrowBookCommand;
import fu.edu.mss301.digilib.loan.domain.aggregate.Loan;
import fu.edu.mss301.digilib.loan.domain.entity.SagaOutbox;
import fu.edu.mss301.digilib.loan.domain.repository.LoanRepository;
import fu.edu.mss301.digilib.loan.domain.repository.SagaOutboxRepository;
import fu.edu.mss301.digilib.loan.domain.vo.LoanStatus;
import fu.edu.mss301.digilib.loan.domain.vo.OutboxStatus;
import fu.edu.mss301.digilib.loan.infrastructure.adapter.BookCatalogClientAdapter;
import fu.edu.mss301.digilib.loan.infrastructure.adapter.FineClientAdapter;
import fu.edu.mss301.digilib.loan.infrastructure.adapter.MemberClientAdapter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BorrowBookUseCase {

    private final LoanRepository loanRepository;
    private final SagaOutboxRepository outboxRepository;
    private final MemberClientAdapter memberClient;
    private final BookCatalogClientAdapter catalogClient;
    private final FineClientAdapter fineClient;

    @Transactional
    public Loan handle(BorrowBookCommand command) {
        return loanRepository.findByIdempotencyKey(command.idempotencyKey())
                .orElseGet(() -> borrow(command));
    }

    private Loan borrow(BorrowBookCommand command) {
        // Fine Service is the source of truth for unpaid-fine borrowing eligibility.
        fineClient.assertCanBorrow(command.memberId());
        MemberClientAdapter.MemberPolicy policy = memberClient.getPolicy(command.memberId());
        long activeLoans = loanRepository.countByMemberIdAndStatusIn(
                command.memberId(), List.of(LoanStatus.BORROWED, LoanStatus.OVERDUE));
        if (activeLoans >= policy.borrowingLimit()) {
            throw new IllegalStateException("Member borrowing limit exceeded");
        }

        Long copyId = null;
        try {
            if (!"DIGITAL".equalsIgnoreCase(command.bookType())) {
                copyId = catalogClient.reserveBook(command.bookId());
            }
            Loan loan = Loan.create(
                    command.memberId(),
                    command.bookId(),
                    copyId,
                    command.bookType(),
                    LocalDateTime.now().plusDays(policy.loanPeriodDays()),
                    command.idempotencyKey());
            Loan saved = loanRepository.save(loan);
            outboxRepository.save(event(saved, "LoanCreatedEvent"));
            return saved;
        } catch (RuntimeException exception) {
            if (copyId != null) {
                catalogClient.releaseBook(copyId);
            }
            throw exception;
        }
    }

    private SagaOutbox event(Loan loan, String type) {
        return SagaOutbox.builder()
                .loanId(loan.getLoanId())
                .eventType(type)
                .payload("{\"loanId\":%d,\"memberId\":\"%s\",\"bookId\":%d}"
                        .formatted(loan.getLoanId(), loan.getMemberId(), loan.getBookId()))
                .status(OutboxStatus.PENDING)
                .retryCount(0)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
