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
    private Long memberId;

    @Column(name = "book_id", nullable = false)
    private Long bookId;

    @Column(name = "book_type", nullable = false)
    private String bookType;

    @Column(name = "status", nullable = false)
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

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id")
    private final List<LoanStatusHistory> histories = new ArrayList<>();

    private Loan() {
    }

    public static Loan create(
            Long memberId,
            Long bookId,
            String bookType,
            LocalDateTime dueDate) {

        if (memberId == null)
            throw new IllegalArgumentException("member required");

        if (bookId == null)
            throw new IllegalArgumentException("book required");

        Loan loan = new Loan();

        loan.memberId = memberId;
        loan.bookId = bookId;
        loan.bookType = bookType;
        loan.status = LoanStatus.BORROWED;
        loan.borrowedAt = LocalDateTime.now();
        loan.dueDate = dueDate;
        loan.renewalCount = 0;
        loan.maxRenewals = 3;
        loan.createdAt = LocalDateTime.now();

        return loan;
    }

    public void renew() {

        if (renewalCount >= maxRenewals)
            throw new IllegalStateException(
                    "Maximum renewals exceeded");

        renewalCount++;
        dueDate = dueDate.plusDays(14);
    }

    public void returnBook() {

        if (status == LoanStatus.RETURNED)
            throw new IllegalStateException(
                    "Book already returned");

        status = LoanStatus.RETURNED;
        returnedAt = LocalDateTime.now();
    }

    public void markOverdue() {

        if (status == LoanStatus.BORROWED &&
                LocalDateTime.now().isAfter(dueDate)) {

            status = LoanStatus.OVERDUE;
        }
    }
}