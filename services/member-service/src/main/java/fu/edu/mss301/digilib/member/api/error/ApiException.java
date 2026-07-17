package fu.edu.mss301.digilib.member.api.error;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class ApiException extends ResponseStatusException {

    private final String code;

    public ApiException(HttpStatus status, String code, String message) {
        super(status, message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
