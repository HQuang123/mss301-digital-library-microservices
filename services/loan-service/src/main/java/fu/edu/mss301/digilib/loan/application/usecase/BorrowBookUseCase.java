package fu.edu.mss301.digilib.loan.application.usecase;

import fu.edu.mss301.digilib.loan.api.dto.BorrowEligibilityResponse;
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
import fu.edu.mss301.digilib.loan.infrastructure.adapter.NotificationClientAdapter;
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
    private final NotificationClientAdapter notificationClient;

    @Transactional
    public Loan handle(BorrowBookCommand command) {
        return loanRepository.findByIdempotencyKey(command.idempotencyKey())
                .orElseGet(() -> borrow(command));
    }

    @Transactional(readOnly = true)
    public BorrowEligibilityResponse checkEligibility(String memberId) {
        if (memberId == null || memberId.isBlank()) {
            throw new IllegalArgumentException("Không xác định được thành viên đang đăng nhập");
        }

        fineClient.assertCanBorrow(memberId);
        MemberClientAdapter.MemberDetails member = memberClient.getMember(memberId);
        long activeLoans = loanRepository.countByMemberIdAndStatusIn(
                memberId, List.of(LoanStatus.BORROWED, LoanStatus.OVERDUE));
        if (activeLoans >= member.borrowingLimit()) {
            throw new IllegalStateException("Bạn đã đạt hạn mức mượn sách");
        }

        return new BorrowEligibilityResponse(
                true,
                activeLoans,
                member.borrowingLimit(),
                member.borrowingLimit() - activeLoans,
                member.loanPeriodDays(),
                "Member is eligible to create a borrow request");
    }

    @Transactional
    public Loan approve(Loan request, String reviewerId) {
        if (request.getStatus() != LoanStatus.PENDING) {
            throw new IllegalStateException("Yêu cầu mượn không còn ở trạng thái chờ duyệt");
        }

        checkEligibility(request.getMemberId());
        MemberClientAdapter.MemberDetails member = memberClient.getMember(request.getMemberId());

        Long copyId = null;
        try {
            if (!"DIGITAL".equalsIgnoreCase(request.getBookType())) {
                copyId = catalogClient.reserveBook(request.getBookId());
            }
            request.approve(copyId, LocalDateTime.now().plusDays(member.loanPeriodDays()), reviewerId);
            Loan saved = loanRepository.save(request);
            outboxRepository.save(event(saved, "LoanCreatedEvent"));
            BookCatalogClientAdapter.BookDetails book = catalogClient.getBookDetails(saved.getBookId());
            notificationClient.sendBorrowConfirmation(
                    saved.getLoanId(), saved.getMemberId(), member.email(), book.title(), saved.getDueDate());
            return saved;
        } catch (RuntimeException exception) {
            if (copyId != null) {
                catalogClient.releaseBook(copyId);
            }
            throw exception;
        }
    }

    private Loan borrow(BorrowBookCommand command) {
        // Fine Service is the source of truth for unpaid-fine borrowing eligibility.
        checkEligibility(command.memberId());
        MemberClientAdapter.MemberDetails member = memberClient.getMember(command.memberId());

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
                    LocalDateTime.now().plusDays(member.loanPeriodDays()),
                    command.idempotencyKey());
            Loan saved = loanRepository.save(loan);
            outboxRepository.save(event(saved, "LoanCreatedEvent"));
            BookCatalogClientAdapter.BookDetails book = catalogClient.getBookDetails(saved.getBookId());
            notificationClient.sendBorrowConfirmation(
                    saved.getLoanId(), saved.getMemberId(), member.email(), book.title(), saved.getDueDate());
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
