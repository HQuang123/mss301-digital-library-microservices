package fu.edu.mss301.digilib.catalog.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

@Getter @Setter
@Entity
@NoArgsConstructor @AllArgsConstructor
@Builder
@Table(name = "categories")
@SQLRestriction("is_deleted = false")
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_id")
    private Long categoryId;

    @Column(name = "category_name", nullable = false, length = 100)
    private String categoryName;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

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
