package fu.edu.mss301.digilib.catalog.api.dto;

import fu.edu.mss301.digilib.catalog.domain.entity.Classification;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ClassificationResponse {
    private Long classificationId;
    private String classificationSystem;
    private String classificationName;
    private Integer classificationCode;

    public static ClassificationResponse from(Classification classification) {
        return new ClassificationResponse(
                classification.getClassificationId(),
                classification.getClassificationSystem(),
                classification.getClassificationName(),
                classification.getClassificationCode()
        );
    }
}
