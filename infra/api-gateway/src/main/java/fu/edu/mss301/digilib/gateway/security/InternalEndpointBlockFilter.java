package fu.edu.mss301.digilib.gateway.security;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;
import reactor.core.publisher.Mono;

@Component
public class InternalEndpointBlockFilter implements GlobalFilter, Ordered {

    private static final PathPatternParser PARSER = new PathPatternParser();
    private static final PathPattern INTERNAL_MEMBER_PATH =
            PARSER.parse("/api/v1/members/internal/**");
    private static final PathPattern NOTIFICATION_CREATE_PATH =
            PARSER.parse("/api/notifications");
    private static final PathPattern NOTIFICATION_RETURN_CONFIRMATION_PATH =
            PARSER.parse("/api/notifications/return-confirmation");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        var path = exchange.getRequest().getPath().pathWithinApplication();
        var method = exchange.getRequest().getMethod();

        boolean blocked = INTERNAL_MEMBER_PATH.matches(path)
                || (org.springframework.http.HttpMethod.POST.equals(method)
                    && (NOTIFICATION_CREATE_PATH.matches(path)
                        || NOTIFICATION_RETURN_CONFIRMATION_PATH.matches(path)));

        if (!blocked) {
            return chain.filter(exchange);
        }

        // Do not reveal that a private service-to-service endpoint exists.
        exchange.getResponse().setStatusCode(HttpStatus.NOT_FOUND);
        return exchange.getResponse().setComplete();
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
