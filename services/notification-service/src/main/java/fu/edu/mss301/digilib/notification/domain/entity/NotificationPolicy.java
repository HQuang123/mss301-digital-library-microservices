package fu.edu.mss301.digilib.notification.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "notification_policy")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "event_type", length = 255, nullable = false)
    private String eventType;

    @Column(name = "subject_template", length = 255, nullable = false)
    private String subjectTemplate;

    @Column(name = "body_template", length = 255, nullable = false)
    private String bodyTemplate;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "create_at", nullable = false, updatable = false)
    private LocalDateTime createAt;

    @Column(name = "update_at")
    private LocalDateTime updateAt;

    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL, orphanRemoval = false)
    private List<NotificationLog> logs = new ArrayList<>();

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