package fu.edu.mss301.digilib.notification.application.command;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
public class NotificationCommand {

    private final String eventType;
    private final String studentId;
    private final String studentEmail;
    private final Map<String, String> templateVariables;
}
