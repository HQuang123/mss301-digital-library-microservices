package fu.edu.mss301.digilib.notification.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReturnConfirmationRequest {

    @NotBlank
    private String studentId;

    @NotBlank
    @Email
    private String studentEmail;

    @NotBlank
    private String bookTitle;

    private LocalDateTime returnedAt;
}
