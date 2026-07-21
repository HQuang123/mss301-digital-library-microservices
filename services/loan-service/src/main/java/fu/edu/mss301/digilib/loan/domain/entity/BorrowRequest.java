package fu.edu.mss301.digilib.loan.domain.entity;

import fu.edu.mss301.digilib.loan.domain.aggregate.Loan;
import fu.edu.mss301.digilib.loan.domain.vo.BorrowRequestStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Locale;

@Entity
@Getter
@Table(name = "borrow_requests")
public class BorrowRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "request_id")
    private Long requestId;

    @Column(name = "member_id", nullable = false)
    private String memberId;

    @Column(name = "book_id", nullable = false)
    private Long bookId;

    @Column(name = "book_type", nullable = false, length = 20)
    private String bookType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private BorrowRequestStatus status;

    @Column(name = "idempotency_key", nullable = false, unique = true)
    private String idempotencyKey;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id", unique = true)
    private Loan loan;

    @Column(name = "requested_at", nullable = false, updatable = false)
    private LocalDateTime requestedAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "processed_by")
    private String processedBy;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Version
    private Long version;

    protected BorrowRequest() {
    }

    public static BorrowRequest create(String memberId, Long bookId, String bookType, String idempotencyKey) {
        if (memberId == null || memberId.isBlank()) {
            throw new IllegalArgumentException("Authenticated member is required");
        }
        if (bookId == null) {
            throw new IllegalArgumentException("Book is required");
        }
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("Idempotency key is required");
        }

        String normalizedBookType = bookType == null || bookType.isBlank()
                ? "PHYSICAL"
                : bookType.toUpperCase(Locale.ROOT);
        if (!normalizedBookType.equals("PHYSICAL") && !normalizedBookType.equals("DIGITAL")) {
            throw new IllegalArgumentException("Book type must be PHYSICAL or DIGITAL");
        }

        BorrowRequest request = new BorrowRequest();
        request.memberId = memberId;
        request.bookId = bookId;
        request.bookType = normalizedBookType;
        request.status = BorrowRequestStatus.PENDING;
        request.idempotencyKey = idempotencyKey;
        request.requestedAt = LocalDateTime.now();
        return request;
    }

    public void approve(Loan approvedLoan, String actorId) {
        ensurePending();
        if (approvedLoan == null) {
            throw new IllegalArgumentException("Approved loan is required");
        }
        status = BorrowRequestStatus.APPROVED;
        loan = approvedLoan;
        processedAt = LocalDateTime.now();
        processedBy = normalizeActor(actorId);
    }

    public void reject(String reason, String actorId) {
        ensurePending();
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Rejection reason is required");
        }
        status = BorrowRequestStatus.REJECTED;
        rejectionReason = reason.trim();
        processedAt = LocalDateTime.now();
        processedBy = normalizeActor(actorId);
    }

    public void cancel(String actorId) {
        ensurePending();
        status = BorrowRequestStatus.CANCELLED;
        processedAt = LocalDateTime.now();
        processedBy = normalizeActor(actorId);
    }

    private void ensurePending() {
        if (status != BorrowRequestStatus.PENDING) {
            throw new IllegalStateException("Only a pending borrow request can be processed");
        }
    }

    private String normalizeActor(String actorId) {
        return actorId == null || actorId.isBlank() ? "SYSTEM" : actorId;
    }
}
