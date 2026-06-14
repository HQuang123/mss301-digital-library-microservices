package fu.edu.mss301.digilib.catalog.application.command;

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
public class ClassificationCommand {

    private Long classificationId;
    private String classificationSystem;
    private String classificationName;
    private Integer classificationCode;
    private String keyword;
}
