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

    @Column(name = "borrowed_at")
    private LocalDateTime borrowedAt;

    @Column(name = "due_date")
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

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "reviewed_by")
    private String reviewedBy;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Version
    private Long version;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id")
    private final List<LoanStatusHistory> histories = new ArrayList<>();

    protected Loan() {
    }

    public static Loan create(
            String memberId,
            Long bookId,
            Long copyId,
            String bookType,
            LocalDateTime dueDate,
            String idempotencyKey) {

        if (memberId == null || memberId.isBlank())
            throw new IllegalArgumentException("Thiếu thông tin thành viên");

        if (bookId == null)
            throw new IllegalArgumentException("Thiếu thông tin sách");

        if (dueDate == null || !dueDate.isAfter(LocalDateTime.now()))
            throw new IllegalArgumentException("Hạn trả phải là một thời điểm trong tương lai");

        if (idempotencyKey == null || idempotencyKey.isBlank())
            throw new IllegalArgumentException("Thiếu mã chống trùng lặp yêu cầu");

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

    public static Loan request(String memberId, Long bookId, String bookType, String idempotencyKey) {
        if (memberId == null || memberId.isBlank())
            throw new IllegalArgumentException("Thiếu thông tin thành viên");
        if (bookId == null)
            throw new IllegalArgumentException("Thiếu thông tin sách");
        if (idempotencyKey == null || idempotencyKey.isBlank())
            throw new IllegalArgumentException("Thiếu mã chống trùng lặp yêu cầu");

        LocalDateTime now = LocalDateTime.now();
        Loan loan = new Loan();
        loan.memberId = memberId;
        loan.bookId = bookId;
        loan.bookType = bookType == null || bookType.isBlank() ? "PHYSICAL" : bookType.toUpperCase();
        loan.status = LoanStatus.PENDING;
        loan.renewalCount = 0;
        loan.maxRenewals = 3;
        loan.idempotencyKey = idempotencyKey;
        loan.createdAt = now;
        loan.updatedAt = now;
        loan.addHistory(null, LoanStatus.PENDING, memberId, "Borrow requested");
        return loan;
    }

    public void approve(Long reservedCopyId, LocalDateTime approvedDueDate, String reviewerId) {
        requirePending();
        if (approvedDueDate == null || !approvedDueDate.isAfter(LocalDateTime.now()))
            throw new IllegalArgumentException("Hạn trả phải là một thời điểm trong tương lai");
        if (!"DIGITAL".equalsIgnoreCase(bookType) && reservedCopyId == null)
            throw new IllegalArgumentException("Thiếu thông tin bản sao sách vật lý");

        LocalDateTime now = LocalDateTime.now();
        LoanStatus previousStatus = status;
        copyId = reservedCopyId;
        status = LoanStatus.BORROWED;
        borrowedAt = now;
        dueDate = approvedDueDate;
        reviewedAt = now;
        reviewedBy = reviewerId;
        updatedAt = now;
        addHistory(previousStatus, status, reviewerId, "Borrow request approved");
    }

    public void reject(String reviewerId, String reason) {
        requirePending();
        if (reason == null || reason.isBlank())
            throw new IllegalArgumentException("Vui lòng nhập lý do từ chối");
        status = LoanStatus.REJECTED;
        reviewedAt = LocalDateTime.now();
        reviewedBy = reviewerId;
        rejectionReason = reason.trim();
        updatedAt = reviewedAt;
        addHistory(LoanStatus.PENDING, status, reviewerId,
                rejectionReason.substring(0, Math.min(rejectionReason.length(), 255)));
    }

    public void cancel(String memberId) {
        requirePending();
        status = LoanStatus.CANCELLED;
        reviewedAt = LocalDateTime.now();
        reviewedBy = memberId;
        updatedAt = reviewedAt;
        addHistory(LoanStatus.PENDING, status, memberId, "Borrow request cancelled");
    }

    private void requirePending() {
        if (status != LoanStatus.PENDING)
            throw new IllegalStateException("Yêu cầu mượn không còn ở trạng thái chờ duyệt");
    }

    public void renew(String changedBy) {

        if (status != LoanStatus.BORROWED)
            throw new IllegalStateException("Chỉ có thể gia hạn phiếu đang mượn");

        if (renewalCount >= maxRenewals)
            throw new IllegalStateException(
                    "Đã đạt số lần gia hạn tối đa");

        renewalCount++;
        dueDate = dueDate.plusDays(14);
        updatedAt = LocalDateTime.now();
        addHistory(status, status, changedBy, "Loan renewed");
    }

    public void returnBook(String changedBy) {

        if (status != LoanStatus.BORROWED && status != LoanStatus.OVERDUE)
            throw new IllegalStateException(
                    "Chỉ có thể trả sách đang mượn hoặc đã quá hạn");

        LoanStatus previousStatus = status;
        status = LoanStatus.RETURNED;
        returnedAt = LocalDateTime.now();
        updatedAt = returnedAt;
        addHistory(previousStatus, status, changedBy, "Book returned");
    }

    public void markLost(String changedBy) {
        if (status != LoanStatus.BORROWED && status != LoanStatus.OVERDUE) {
            throw new IllegalStateException("Chỉ có thể báo mất sách đang mượn hoặc đã quá hạn");
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
            addHistory(previousStatus, status, "SYSTEM", "Đã quá hạn trả sách");
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
