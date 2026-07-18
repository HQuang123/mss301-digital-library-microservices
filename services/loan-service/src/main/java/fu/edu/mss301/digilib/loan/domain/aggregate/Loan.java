package fu.edu.mss301.digilib.loan.domain.aggregate;

import fu.edu.mss301.digilib.loan.domain.entity.LoanStatusHistory;
import fu.edu.mss301.digilib.loan.domain.vo.LoanStatus;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Table(name = "loans")
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "loan_id")
    private Long loanId;

    @Column(name = "member_id", nullable = false)
    private String memberId;

    @Column(name = "book_id", nullable = false)
    private Long bookId;

    @Column(name = "copy_id")
    private Long copyId;

    @Column(name = "book_type", nullable = false)
    private String bookType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private LoanStatus status;

    @Column(name = "borrowed_at", nullable = false)
    private LocalDateTime borrowedAt;

    @Column(name = "due_date", nullable = false)
    private LocalDateTime dueDate;

    @Column(name = "returned_at")
    private LocalDateTime returnedAt;

    @Column(name = "renewal_count", nullable = false)
    private Integer renewalCount;

    @Column(name = "max_renewals", nullable = false)
    private Integer maxRenewals;

    @Column(name = "idempotency_key", nullable = false, unique = true)
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    private Long version;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id")
    private final List<LoanStatusHistory> histories = new ArrayList<>();

    private Loan() {
    }

    public static Loan create(
            String memberId,
            Long bookId,
            Long copyId,
            String bookType,
            LocalDateTime dueDate,
            String idempotencyKey) {

        if (memberId == null || memberId.isBlank())
            throw new IllegalArgumentException("member required");

        if (bookId == null)
            throw new IllegalArgumentException("book required");

        if (dueDate == null || !dueDate.isAfter(LocalDateTime.now()))
            throw new IllegalArgumentException("due date must be in the future");

        if (idempotencyKey == null || idempotencyKey.isBlank())
            throw new IllegalArgumentException("idempotency key required");

        Loan loan = new Loan();

        loan.memberId = memberId;
        loan.bookId = bookId;
        loan.copyId = copyId;
        loan.bookType = bookType == null || bookType.isBlank() ? "PHYSICAL" : bookType.toUpperCase();
        loan.status = LoanStatus.BORROWED;
        loan.borrowedAt = LocalDateTime.now();
        loan.dueDate = dueDate;
        loan.renewalCount = 0;
        loan.maxRenewals = 3;
        loan.idempotencyKey = idempotencyKey;
        loan.createdAt = loan.borrowedAt;
        loan.updatedAt = loan.borrowedAt;
        loan.addHistory(null, LoanStatus.BORROWED, "SYSTEM", "Loan created");

        return loan;
    }

    public void renew(String changedBy) {

        if (status != LoanStatus.BORROWED)
            throw new IllegalStateException("Only an active loan can be renewed");

        if (renewalCount >= maxRenewals)
            throw new IllegalStateException(
                    "Maximum renewals exceeded");

        renewalCount++;
        dueDate = dueDate.plusDays(14);
        updatedAt = LocalDateTime.now();
        addHistory(status, status, changedBy, "Loan renewed");
    }

    public void returnBook(String changedBy) {

        if (status != LoanStatus.BORROWED && status != LoanStatus.OVERDUE)
            throw new IllegalStateException(
                    "Only an active or overdue loan can be returned");

        LoanStatus previousStatus = status;
        status = LoanStatus.RETURNED;
        returnedAt = LocalDateTime.now();
        updatedAt = returnedAt;
        addHistory(previousStatus, status, changedBy, "Book returned");
    }

    public void markLost(String changedBy) {
        if (status != LoanStatus.BORROWED && status != LoanStatus.OVERDUE) {
            throw new IllegalStateException("Only an active or overdue loan can be marked as lost");
        }
        LoanStatus previousStatus = status;
        status = LoanStatus.LOST;
        updatedAt = LocalDateTime.now();
        addHistory(previousStatus, status, changedBy, "Book reported lost");
    }

    public void markOverdue() {

        if (status == LoanStatus.BORROWED &&
                LocalDateTime.now().isAfter(dueDate)) {

            LoanStatus previousStatus = status;
            status = LoanStatus.OVERDUE;
            updatedAt = LocalDateTime.now();
            addHistory(previousStatus, status, "SYSTEM", "Due date exceeded");
        }
    }

    private void addHistory(LoanStatus from, LoanStatus to, String changedBy, String reason) {
        histories.add(LoanStatusHistory.builder()
                .fromStatus(from)
                .toStatus(to)
                .changedBy(changedBy == null || changedBy.isBlank() ? "SYSTEM" : changedBy)
                .reason(reason)
                .changedAt(LocalDateTime.now())
                .build());
    }
}
