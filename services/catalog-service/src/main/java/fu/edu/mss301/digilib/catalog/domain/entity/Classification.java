package fu.edu.mss301.digilib.catalog.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

@Getter @Setter
@Entity
@NoArgsConstructor @AllArgsConstructor
@Builder
@Table(name = "classifications")
@SQLRestriction("is_deleted = false")
public class Classification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "classification_id")
    private Long classificationId;

    @Column(name = "classification_system", nullable = false, length = 50)
    private String classificationSystem;

    @Column(name = "classification_name", nullable = false, length = 100)
    private String classificationName;

    @Column(name = "classification_code", nullable = false)
    private Integer classificationCode;

    @Builder.Default
    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    public void softDelete() {
        this.isDeleted = true;
    }

    public void restore() {
        this.isDeleted = false;
    }
}
