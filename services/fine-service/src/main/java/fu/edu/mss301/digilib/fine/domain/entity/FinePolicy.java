package fu.edu.mss301.digilib.fine.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "fine_policy")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinePolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "daily_rate", nullable = false)
    private Double dailyRate;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "lost_threshold_days", nullable = false)
    private Integer lostThresholdDays;

    @Column(name = "lost_penalty", nullable = false)
    private Double lostPenalty;

    @Column(name = "create_at", nullable = false, updatable = false)
    private LocalDateTime createAt;

    @Column(name = "update_at")
    private LocalDateTime updateAt;

    @PrePersist
    protected void onCreate() {
        this.createAt = LocalDateTime.now();
        this.updateAt = LocalDateTime.now();

        if (this.isActive == null) {
            this.isActive = true;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updateAt = LocalDateTime.now();
    }
}