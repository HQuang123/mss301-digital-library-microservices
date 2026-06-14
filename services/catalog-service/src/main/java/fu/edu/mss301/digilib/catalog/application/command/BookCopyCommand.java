package fu.edu.mss301.digilib.catalog.application.command;

import fu.edu.mss301.digilib.catalog.domain.entity.BookCopy;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class BookCopyCommand {

    private Long bookId;
    private Long copyId;
    private String barcode;
    private String shelfLocation;
    private LocalDate acquisitionDate;
    private BookCopy.CopyStatus copyStatus;
    private String keyword;
    private Integer userId;
}
