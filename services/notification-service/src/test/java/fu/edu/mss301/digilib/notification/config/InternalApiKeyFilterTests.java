package fu.edu.mss301.digilib.notification.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InternalApiKeyFilterTests {

    private static final String KEY = "test-only-internal-api-key-with-more-than-32-characters";
    private final InternalApiKeyFilter filter = new InternalApiKeyFilter(KEY);

    @Test
    void rejectsPlaceholderOrShortConfiguration() {
        assertThatThrownBy(() -> new InternalApiKeyFilter("replace-with-a-random-key"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("INTERNAL_API_KEY");
        assertThatThrownBy(() -> new InternalApiKeyFilter("too-short"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("INTERNAL_API_KEY");
    }

    @Test
    void rejectsProtectedPostMappingWhenApiKeyIsMissing() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/notifications");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean downstream = new AtomicBoolean(false);

        filter.doFilter(request, response, tracking(downstream));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_JSON_VALUE);
        assertThat(downstream).isFalse();
    }

    @Test
    void rejectsProtectedPostMappingWhenApiKeyIsWrong() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/notifications");
        request.addHeader(InternalApiKeyFilter.HEADER_NAME, "wrong-internal-api-key-with-more-than-32-characters");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean downstream = new AtomicBoolean(false);

        filter.doFilter(request, response, tracking(downstream));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(downstream).isFalse();
    }

    @Test
    void rejectsReturnConfirmationPathWithWrongKey() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/notifications/return-confirmation");
        request.addHeader(InternalApiKeyFilter.HEADER_NAME, "wrong-internal-api-key-with-more-than-32-characters");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean downstream = new AtomicBoolean(false);

        filter.doFilter(request, response, tracking(downstream));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(downstream).isFalse();
    }

    @Test
    void allowsProtectedPathWhenApiKeyMatches() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/notifications");
        request.addHeader(InternalApiKeyFilter.HEADER_NAME, KEY);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean downstream = new AtomicBoolean(false);

        filter.doFilter(request, response, tracking(downstream));

        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
        assertThat(downstream).isTrue();
    }

    @Test
    void doesNotGateOtherPaths() throws ServletException, IOException {
        // e.g. /api/notifications/me requires a user JWT and is gated by Spring Security, not here.
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/notifications/me");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean downstream = new AtomicBoolean(false);

        filter.doFilter(request, response, tracking(downstream));

        assertThat(downstream).isTrue();
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
    }

    @Test
    void doesNotGateJobsPath() throws ServletException, IOException {
        // /api/notifications/jobs/** is role-restricted at the Spring Security layer (ADMIN/LIBRARIAN).
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/notifications/jobs/due-soon/run");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean downstream = new AtomicBoolean(false);

        filter.doFilter(request, response, tracking(downstream));

        assertThat(downstream).isTrue();
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
    }

    @Test
    void doesNotGateGetOnNotifications() throws ServletException, IOException {
        // GET /api/notifications is admin-only; not an internal service-to-service call.
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/notifications");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean downstream = new AtomicBoolean(false);

        filter.doFilter(request, response, tracking(downstream));

        assertThat(downstream).isTrue();
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
    }

    private FilterChain tracking(AtomicBoolean downstreamCalled) {
        return (request, response) -> downstreamCalled.set(true);
    }
}