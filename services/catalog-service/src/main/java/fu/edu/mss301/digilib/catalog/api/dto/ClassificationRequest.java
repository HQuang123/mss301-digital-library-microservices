package fu.edu.mss301.digilib.catalog.api.dto;

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
public class ClassificationRequest {
    private String classificationSystem;
    private String classificationName;
    private Integer classificationCode;
}
