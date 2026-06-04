package fu.edu.mss301.digilib.loan.domain.entity;

import fu.edu.mss301.digilib.loan.domain.vo.SagaType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "saga_log")
public class SagaLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "saga_id")
    private Long sagaId;

    @Column(name = "loan_id", nullable = false)
    private Long loanId;

    @Enumerated(EnumType.STRING)
    @Column(name = "saga_type", nullable = false)
    private SagaType sagaType;

    @Column(name = "current_step", nullable = false)
    private String currentStep;

    @Enumerated(EnumType.STRING)
    @Column(name = "overall_status", nullable = false)
    private String overallStatus;

    @Column(name = "compensations", nullable = false)
    private Integer compensations;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

}