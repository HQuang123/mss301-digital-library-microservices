package fu.edu.mss301.digilib.gateway.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fu.edu.mss301.digilib.gateway.api.error.ApiErrorResponse;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Component
public class GatewaySecurityErrorWriter implements ServerAuthenticationEntryPoint, ServerAccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public GatewaySecurityErrorWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> commence(ServerWebExchange exchange, AuthenticationException exception) {
        return write(exchange, HttpStatus.UNAUTHORIZED, "AUTHENTICATION_REQUIRED",
                "Cần cung cấp Bearer token hợp lệ để truy cập tài nguyên này.");
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, org.springframework.security.access.AccessDeniedException exception) {
        return write(exchange, HttpStatus.FORBIDDEN, "ACCESS_DENIED",
                "Bạn không có quyền truy cập tài nguyên này.");
    }

    /**
     * Renders a structured JSON error response. Exposed so custom security
     * filters (e.g. {@link OnboardingRequiredFilter}) can reuse the same wire
     * shape used by the auth entry point and access-denied handler.
     */
    public Mono<Void> write(ServerWebExchange exchange, HttpStatus status, String code, String message) {
        return writeInternal(exchange, status, code, message);
    }

    private Mono<Void> writeInternal(ServerWebExchange exchange, HttpStatus status, String code, String message) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        exchange.getResponse().getHeaders().setCacheControl(CacheControl.noStore());
        if (status == HttpStatus.UNAUTHORIZED) {
            exchange.getResponse().getHeaders().set(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        }

        ApiErrorResponse body = new ApiErrorResponse(
                Instant.now(),
                status.value(),
                status == HttpStatus.UNAUTHORIZED ? "Chưa xác thực" : "Không có quyền truy cập",
                code,
                message,
                exchange.getRequest().getPath().value(),
                exchange.getRequest().getId()
        );

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(body);
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
            return exchange.getResponse().writeWith(Mono.just(buffer));
        } catch (JsonProcessingException serializationError) {
            return exchange.getResponse().setComplete();
        }
    }
}
