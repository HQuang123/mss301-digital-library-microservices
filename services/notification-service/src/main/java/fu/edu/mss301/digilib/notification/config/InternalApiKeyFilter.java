package fu.edu.mss301.digilib.notification.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

/**
 * Authenticates inter-service calls to internal notification endpoints using a
 * shared {@code X-Internal-Api-Key} header.  Mirrors the existing
 * {@code InternalApiKeyWebFilter} in member-service so both services enforce
 * the same constant-time comparison and the same startup guard.
 *
 * <p>The gateway's {@code InternalEndpointBlockFilter} blocks external callers
 * from these paths; this filter is the defense-in-depth backstop for cases
 * where a service is reachable directly (e.g. via the {@code digilib-network}
 * Docker alias).
 *
 * <p>Protected paths:
 * <ul>
 *   <li>{@code POST /api/notifications}</li>
 *   <li>{@code POST /api/notifications/return-confirmation}</li>
 * </ul>
 *
 * <p>Other paths (e.g. {@code /api/notifications/jobs/**} which requires
 * ADMIN/LIBRARIAN at the Spring Security layer, or {@code /api/notifications/me}
 * which requires a user JWT) are not intercepted here.
 */
@Component
public class InternalApiKeyFilter extends OncePerRequestFilter {

    static final String HEADER_NAME = "X-Internal-Api-Key";
    private static final int MINIMUM_KEY_LENGTH = 32;

    private final byte[] expectedApiKey;
    private final List<ProtectedPath> protectedPaths = List.of(
            new ProtectedPath("POST", "/api/notifications"),
            new ProtectedPath("POST", "/api/notifications/return-confirmation")
    );

    public InternalApiKeyFilter(@Value("${services.internal-api-key:}") String internalApiKey) {
        if (!StringUtils.hasText(internalApiKey)
                || internalApiKey.length() < MINIMUM_KEY_LENGTH
                || internalApiKey.startsWith("replace-with-")) {
            throw new IllegalStateException(
                    "INTERNAL_API_KEY must be a non-placeholder secret with at least 32 characters");
        }
        this.expectedApiKey = internalApiKey.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!isProtected(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String supplied = request.getHeader(HEADER_NAME);
        if (!matches(supplied)) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("Cache-Control", "no-store");
            response.getOutputStream().write(
                    ("{\"code\":\"INVALID_INTERNAL_API_KEY\",\"message\":"
                            + "\"A valid X-Internal-Api-Key header is required for this endpoint.\"}")
                            .getBytes(StandardCharsets.UTF_8));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isProtected(HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getRequestURI();
        for (ProtectedPath p : protectedPaths) {
            if (p.matches(method, path)) {
                return true;
            }
        }
        return false;
    }

    private boolean matches(String suppliedApiKey) {
        if (suppliedApiKey == null) {
            return false;
        }
        byte[] supplied = suppliedApiKey.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expectedApiKey, supplied);
    }

    private record ProtectedPath(String method, String path) {
        boolean matches(String requestMethod, String requestPath) {
            return method.equalsIgnoreCase(requestMethod) && path.equals(requestPath);
        }
    }
}