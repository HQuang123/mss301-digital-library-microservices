package fu.edu.mss301.digilib.loan.application.usecase;

import fu.edu.mss301.digilib.loan.domain.aggregate.Loan;
import fu.edu.mss301.digilib.loan.domain.entity.SagaOutbox;
import fu.edu.mss301.digilib.loan.domain.repository.LoanRepository;
import fu.edu.mss301.digilib.loan.domain.repository.SagaOutboxRepository;
import fu.edu.mss301.digilib.loan.domain.vo.OutboxStatus;
import fu.edu.mss301.digilib.loan.domain.vo.LoanStatus;
import fu.edu.mss301.digilib.loan.infrastructure.adapter.BookCatalogClientAdapter;
import fu.edu.mss301.digilib.loan.infrastructure.adapter.FineClientAdapter;
import fu.edu.mss301.digilib.loan.infrastructure.adapter.MemberClientAdapter;
import fu.edu.mss301.digilib.loan.infrastructure.adapter.NotificationClientAdapter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ManageLoanUseCase {

    private final LoanRepository loanRepository;
    private final SagaOutboxRepository outboxRepository;
    private final BookCatalogClientAdapter catalogClient;
    private final MemberClientAdapter memberClient;
    private final FineClientAdapter fineClient;
    private final NotificationClientAdapter notificationClient;

    @Transactional(readOnly = true)
    public Loan findById(Long loanId) {
        return loanRepository.findById(loanId)
                .orElseThrow(() -> new IllegalArgumentException("Loan not found: " + loanId));
    }

    @Transactional(readOnly = true)
    public Page<Loan> findAll(Pageable pageable) {
        return loanRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public List<Loan> findByMember(String memberId) {
        return loanRepository.findByMemberIdOrderByBorrowedAtDesc(memberId);
    }

    @Transactional
    public Loan returnBook(Long loanId, String changedBy) {
        Loan loan = findById(loanId);
        ensureActive(loan);
        LocalDateTime returnedAt = LocalDateTime.now();
        long overdueDays = overdueDays(loan.getDueDate(), returnedAt);
        IntegrationDetails details = integrationDetails(loan);

        if (overdueDays > 0) {
            fineClient.createOverdueReturnFine(
                    details.fineContext(), returnedAt.toLocalDate(), overdueDays);
        }

        loan.returnBook(changedBy);
        Loan saved = loanRepository.save(loan);
        catalogClient.releaseBook(saved.getCopyId());
        outboxRepository.save(event(saved, overdueDays > 0 ? "BookReturnedLateEvent" : "BookReturnedEvent"));
        notificationClient.sendReturnConfirmation(
                saved.getMemberId(), details.memberEmail(), details.bookTitle(), saved.getReturnedAt());
        return saved;
    }

    @Transactional
    public Loan renew(Long loanId, String changedBy) {
        Loan loan = findById(loanId);
        loan.renew(changedBy);
        Loan saved = loanRepository.save(loan);
        outboxRepository.save(event(saved, "LoanRenewedEvent"));
        return saved;
    }

    @Transactional
    public Loan reportLost(Long loanId, String changedBy) {
        Loan loan = findById(loanId);
        ensureActive(loan);
        LocalDateTime reportedAt = LocalDateTime.now();
        long overdueDays = overdueDays(loan.getDueDate(), reportedAt);
        IntegrationDetails details = integrationDetails(loan);

        fineClient.createLostBookFine(details.fineContext(), overdueDays);
        loan.markLost(changedBy);
        Loan saved = loanRepository.save(loan);
        catalogClient.markLost(saved.getCopyId());
        outboxRepository.save(event(saved, "BookLostEvent"));
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Long> findLoansOverdueByMoreThanDays(int days, LocalDateTime now) {
        return loanRepository.findByStatusInAndDueDateBefore(
                        List.of(LoanStatus.BORROWED, LoanStatus.OVERDUE), now.minusDays(days))
                .stream()
                .map(Loan::getLoanId)
                .toList();
    }

    @Transactional
    public void createOrUpdateThresholdFine(Long loanId, LocalDateTime now) {
        Loan loan = findById(loanId);
        ensureActive(loan);
        long overdueDays = overdueDays(loan.getDueDate(), now);
        if (overdueDays <= 30) {
            return;
        }
        if (loan.getStatus() == LoanStatus.BORROWED) {
            loan.markOverdue();
            loanRepository.save(loan);
        }
        fineClient.createOverdueThresholdFine(integrationDetails(loan).fineContext(), overdueDays);
    }

    private IntegrationDetails integrationDetails(Loan loan) {
        MemberClientAdapter.MemberDetails member = memberClient.getMember(loan.getMemberId());
        BookCatalogClientAdapter.BookDetails book = catalogClient.getBookDetails(loan.getBookId());
        String studentId = member.memberCode() == null || member.memberCode().isBlank()
                ? member.id()
                : member.memberCode();
        FineClientAdapter.FineContext fineContext = new FineClientAdapter.FineContext(
                studentId,
                String.valueOf(loan.getLoanId()),
                String.valueOf(loan.getBookId()),
                loan.getCopyId() == null ? null : String.valueOf(loan.getCopyId()),
                book.title(),
                book.bookValue(),
                loan.getDueDate().toLocalDate());
        return new IntegrationDetails(fineContext, member.email(), book.title());
    }

    private long overdueDays(LocalDateTime dueDate, LocalDateTime actualDate) {
        if (!actualDate.isAfter(dueDate)) {
            return 0;
        }
        return Math.max(1, ChronoUnit.DAYS.between(dueDate.toLocalDate(), actualDate.toLocalDate()));
    }

    private void ensureActive(Loan loan) {
        if (loan.getStatus() != LoanStatus.BORROWED && loan.getStatus() != LoanStatus.OVERDUE) {
            throw new IllegalStateException("Loan is not active: " + loan.getLoanId());
        }
    }

    private record IntegrationDetails(
            FineClientAdapter.FineContext fineContext,
            String memberEmail,
            String bookTitle
    ) {}

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
