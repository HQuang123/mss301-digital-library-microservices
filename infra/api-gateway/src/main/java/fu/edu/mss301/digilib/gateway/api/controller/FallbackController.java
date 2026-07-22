package fu.edu.mss301.digilib.gateway.api.controller;

import io.netty.handler.timeout.ReadTimeoutException;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;

import java.net.URI;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.TimeoutException;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @RequestMapping("/{service}")
    public ResponseEntity<GatewayFallbackResponse> serviceFallback(
            @PathVariable("service") String service,
            ServerWebExchange exchange
    ) {
        Throwable failure = exchange.getAttribute(
                ServerWebExchangeUtils.CIRCUITBREAKER_EXECUTION_EXCEPTION_ATTR);
        boolean timeout = containsTimeout(failure);
        HttpStatus status = timeout ? HttpStatus.GATEWAY_TIMEOUT : HttpStatus.SERVICE_UNAVAILABLE;
        String code = timeout ? "UPSTREAM_TIMEOUT" : "UPSTREAM_UNAVAILABLE";
        String message = timeout
                ? "Dịch vụ xử lý yêu cầu không phản hồi kịp thời."
                : "Dịch vụ xử lý yêu cầu hiện tạm thời không khả dụng.";

        GatewayFallbackResponse body = new GatewayFallbackResponse(
                Instant.now(),
                status.value(),
                timeout ? "Dịch vụ phản hồi quá thời gian" : "Dịch vụ không khả dụng",
                code,
                service,
                originalPath(exchange),
                exchange.getRequest().getId(),
                message
        );

        return ResponseEntity.status(status)
                .header(HttpHeaders.RETRY_AFTER, "3")
                .cacheControl(CacheControl.noStore())
                .body(body);
    }

    private boolean containsTimeout(Throwable failure) {
        Throwable current = failure;
        while (current != null) {
            if (current instanceof TimeoutException || current instanceof ReadTimeoutException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private String originalPath(ServerWebExchange exchange) {
        Set<URI> originalUrls = exchange.getAttribute(
                ServerWebExchangeUtils.GATEWAY_ORIGINAL_REQUEST_URL_ATTR);
        if (originalUrls == null || originalUrls.isEmpty()) {
            return exchange.getRequest().getPath().value();
        }
        return originalUrls.iterator().next().getPath();
    }

    public record GatewayFallbackResponse(
            Instant timestamp,
            int status,
            String error,
            String code,
            String service,
            String path,
            String requestId,
            String message
    ) {
    }
}
