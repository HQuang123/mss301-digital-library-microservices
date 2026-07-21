package fu.edu.mss301.digilib.fine.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Guards /internal/fines/** the same way member-service guards
 * /api/v1/members/internal/** — a shared secret header, checked in
 * constant time, required on every call from Loan Service.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class InternalApiKeyFilter extends OncePerRequestFilter {

    static final String HEADER_NAME = "X-Internal-Api-Key";
    private static final int MINIMUM_KEY_LENGTH = 32;
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();
    private static final String INTERNAL_FINES_PATTERN = "/internal/fines/**";

    private final byte[] expectedApiKey;

    public InternalApiKeyFilter(@Value("${services.internal-api-key}") String internalApiKey) {
        if (!StringUtils.hasText(internalApiKey)
                || internalApiKey.length() < MINIMUM_KEY_LENGTH
                || internalApiKey.startsWith("replace-with-")) {
            throw new IllegalStateException(
                    "INTERNAL_API_KEY must be a non-placeholder secret with at least 32 characters");
        }
        this.expectedApiKey = internalApiKey.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (!PATH_MATCHER.match(INTERNAL_FINES_PATTERN, request.getServletPath())) {
            filterChain.doFilter(request, response);
            return;
        }

        String suppliedApiKey = request.getHeader(HEADER_NAME);
        if (!matches(suppliedApiKey)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean matches(String suppliedApiKey) {
        return suppliedApiKey != null && MessageDigest.isEqual(
                expectedApiKey,
                suppliedApiKey.getBytes(StandardCharsets.UTF_8));
    }
}
