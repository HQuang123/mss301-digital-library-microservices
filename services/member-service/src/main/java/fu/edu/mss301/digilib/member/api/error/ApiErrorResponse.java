package fu.edu.mss301.digilib.member.api.error;

import java.time.Instant;

public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String code,
        String message,
        String path,
        String requestId
) {
}
