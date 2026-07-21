package fu.edu.mss301.digilib.fine.domain.entity;

import fu.edu.mss301.digilib.fine.domain.vo.FineReason;
import fu.edu.mss301.digilib.fine.domain.vo.FineStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "fines",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_fines_loan_id", columnNames = "loan_id")
        },
        indexes = {
                @Index(name = "idx_fines_student_status", columnList = "student_id,status")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Fine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * Many fines can use one fine policy.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_id", nullable = false)
    private FinePolicy finePolicy;

    @Column(name = "loan_id", nullable = false)
    private Long loanId;

    /**
     * Set from the bookId Loan Service sends when the fine is created.
     * Fine Service does not own book data; this is only a reference used
     * to look up the title from Catalog Service when displaying fine history.
     */
    @Column(name = "book_id")
    private Long bookId;

    /**
     * Matches the identifier Loan Service sends (member's Keycloak sub or
     * member code), not a numeric id — Member Service ids are not integers.
     */
    @Column(name = "student_id", nullable = false, length = 100)
    private String studentId;

    /**
     * Nullable: Loan Service does not currently send an email when creating
     * a fine. Populate this once a source (Member Service lookup, or Loan
     * Service passing it through) is wired up.
     */
    @Column(name = "student_email", length = 100)
    private String studentEmail;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", length = 30, nullable = false)
    private FineReason reason;

    @Column(name = "due_date", nullable = false)
    private LocalDateTime dueDate;

    @Column(name = "return_date")
    private LocalDateTime returnDate;

    /**
     * Total amount owed (overdue-day fee + lost penalty if applicable +
     * compensationAmount). This is the single number payment flows charge.
     */
    @Column(name = "amount_due", nullable = false)
    private Long amountDue;

    /**
     * Portion of amountDue that is book-replacement compensation (set only
     * for LOST_BOOK fines). Stored separately purely so history screens can
     * show a breakdown; payment flows still charge amountDue as a whole.
     */
    @Column(name = "compensation_amount", nullable = false)
    private Long compensationAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 50, nullable = false)
    private FineStatus status;

    @Column(name = "waiver_reason", length = 255)
    private String waiverReason;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Version
    private Long version;

    @Column(name = "create_at", nullable = false, updatable = false)
    private LocalDateTime createAt;

    @Column(name = "update_at", nullable = false)
    private LocalDateTime updateAt;

    @PrePersist
    protected void onCreate() {
        this.createAt = LocalDateTime.now();
        this.updateAt = this.createAt;

        if (this.status == null) {
            this.status = FineStatus.PENDING;
        }

        if (this.amountDue == null) {
            this.amountDue = 0L;
        }

        if (this.compensationAmount == null) {
            this.compensationAmount = 0L;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updateAt = LocalDateTime.now();
    }

    public void markPaid(LocalDateTime paymentTime) {
        this.status = FineStatus.PAID;
        this.paidAt = paymentTime;
        this.waiverReason = null;
    }
}
