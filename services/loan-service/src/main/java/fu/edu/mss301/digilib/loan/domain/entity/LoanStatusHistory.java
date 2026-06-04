package fu.edu.mss301.digilib.loan.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "loan_status_history")
public class LoanStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "loan_id", nullable = false)
    private Long loanId;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", nullable = false)
    private String fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false)
    private String toStatus;

    @Column(name = "changed_by", nullable = false)
    private String changedBy;

    @Column(name = "reason", nullable = false)
    private String reason;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

}