package fu.edu.mss301.digilib.member.api.error;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;

class MemberApiExceptionHandlerTests {

    private final MemberApiExceptionHandler handler = new MemberApiExceptionHandler();

    @Test
    void returnsStableStructuredApiError() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/v1/auth/login").build());
        ApiException exception = new ApiException(
                HttpStatus.UNAUTHORIZED,
                "INVALID_CREDENTIALS",
                "Invalid username or password.");

        ResponseEntity<ApiErrorResponse> response = handler.handleApiException(exception, exchange);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getHeaders().getCacheControl()).isEqualTo("no-store");
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("INVALID_CREDENTIALS");
        assertThat(response.getBody().message()).isEqualTo("Invalid username or password.");
        assertThat(response.getBody().path()).isEqualTo("/api/v1/auth/login");
        assertThat(response.getBody().requestId()).isNotBlank();
    }
}
