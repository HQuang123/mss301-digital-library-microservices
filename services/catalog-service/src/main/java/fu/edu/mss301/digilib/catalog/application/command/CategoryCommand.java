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
public class CategoryCommand {

    private Long categoryId;
    private String categoryName;
    private String description;
    private String keyword;
}
