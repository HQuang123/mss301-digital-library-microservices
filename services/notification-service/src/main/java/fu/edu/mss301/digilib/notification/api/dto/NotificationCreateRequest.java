package fu.edu.mss301.digilib.notification.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationCreateRequest {

    @NotBlank
    private String eventType;

    @NotBlank
    private String studentId;

    @NotBlank
    @Email
    private String studentEmail;

    private Map<String, String> templateVariables;
}
