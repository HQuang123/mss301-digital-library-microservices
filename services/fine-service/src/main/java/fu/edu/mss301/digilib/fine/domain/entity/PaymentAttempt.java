package fu.edu.mss301.digilib.fine.domain.entity;

import fu.edu.mss301.digilib.fine.domain.vo.PaymentProvider;
import fu.edu.mss301.digilib.fine.domain.vo.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "payment_attempts",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_payment_attempts_payment_code", columnNames = "payment_code"),
                @UniqueConstraint(name = "uk_payment_attempts_sepay_transaction_id", columnNames = "sepay_transaction_id"),
                @UniqueConstraint(name = "uk_payment_attempts_sepay_reference_code", columnNames = "sepay_reference_code")
        },
        indexes = {
                @Index(name = "idx_payment_attempts_fine_status", columnList = "fine_id,status")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fine_id", nullable = false, updatable = false)
    private Fine fine;

    @Column(name = "payment_code", nullable = false, updatable = false, length = 50)
    private String paymentCode;

    @Column(name = "amount", nullable = false, updatable = false)
    private Long amount;

    @Column(name = "currency", nullable = false, updatable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, updatable = false, length = 20)
    private PaymentProvider provider;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PaymentStatus status;

    @Column(name = "sepay_transaction_id", length = 100)
    private String sepayTransactionId;

    @Column(name = "sepay_reference_code", length = 100)
    private String sepayReferenceCode;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Version
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;

        if (this.provider == null) {
            this.provider = PaymentProvider.SEPAY;
        }

        if (this.status == null) {
            this.status = PaymentStatus.CREATED;
        }

        if (this.currency == null || this.currency.isBlank()) {
            this.currency = "VND";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void markSucceeded(
            String transactionId,
            String referenceCode,
            LocalDateTime paymentTime
    ) {
        this.status = PaymentStatus.SUCCEEDED;
        this.sepayTransactionId = transactionId;
        this.sepayReferenceCode = referenceCode;
        this.paidAt = paymentTime;
    }

    public void markExpired() {
        if (this.status == PaymentStatus.PENDING || this.status == PaymentStatus.CREATED) {
            this.status = PaymentStatus.EXPIRED;
        }
    }
}
