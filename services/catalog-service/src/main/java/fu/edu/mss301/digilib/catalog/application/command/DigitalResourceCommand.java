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
public class DigitalResourceCommand {

    private Long bookId;
    private Long resourceId;
    private String fileFormat;
    private String resourceUrl;
    private String accessPermission;
    private String requesterPermission;
    private String keyword;
    private Integer userId;
}
