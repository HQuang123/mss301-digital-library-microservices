package fu.edu.mss301.digilib.catalog.api.dto;

import fu.edu.mss301.digilib.catalog.domain.entity.BookCopy;
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
public class BookCopyStatusRequest {
    private BookCopy.CopyStatus copyStatus;
    private Integer userId;
}
