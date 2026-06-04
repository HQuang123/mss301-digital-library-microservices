package fu.edu.mss301.digilib.fine.domain.entity;

import fu.edu.mss301.digilib.fine.domain.vo.FineStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "fines")
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
    private Integer loanId;

    @Column(name = "student_id", nullable = false)
    private Integer studentId;

    @Column(name = "student_email", nullable = false, length = 100)
    private String studentEmail;

    @Column(name = "due_date", nullable = false)
    private LocalDateTime dueDate;

    @Column(name = "return_date")
    private LocalDateTime returnDate;

    @Column(name = "amount_due", nullable = false)
    private Double amountDue;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 50, nullable = false)
    private FineStatus status;

    @Column(name = "waiver_reason", length = 255)
    private String waiverReason;

    @Column(name = "create_at", nullable = false, updatable = false)
    private LocalDateTime createAt;

    @Column(name = "update_at")
    private LocalDateTime updateAt;

    @PrePersist
    protected void onCreate() {
        this.createAt = LocalDateTime.now();
        this.updateAt = LocalDateTime.now();

        if (this.status == null) {
            this.status = FineStatus.PENDING;
        }

        if (this.amountDue == null) {
            this.amountDue = 0.0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updateAt = LocalDateTime.now();
    }
}
