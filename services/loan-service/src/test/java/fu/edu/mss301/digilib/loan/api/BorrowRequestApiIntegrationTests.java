package fu.edu.mss301.digilib.loan.api;

import fu.edu.mss301.digilib.loan.domain.repository.BorrowRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BorrowRequestApiIntegrationTests {

    private static final String USER_HEADER = "X-Authenticated-User-Id";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BorrowRequestRepository requestRepository;

    @BeforeEach
    void cleanRequests() {
        requestRepository.deleteAll();
    }

    @Test
    void createsAndReturnsRequestsForAuthenticatedMember() throws Exception {
        mockMvc.perform(post("/api/v1/borrow-requests")
                        .header(USER_HEADER, "member-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "bookId": 10,
                                  "bookType": "PHYSICAL",
                                  "idempotencyKey": "api-request-1"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.memberId").value("member-1"))
                .andExpect(jsonPath("$.status").value("PENDING"));

        mockMvc.perform(get("/api/v1/borrow-requests/me")
                        .header(USER_HEADER, "member-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].bookId").value(10))
                .andExpect(jsonPath("$.content[0].status").value("PENDING"));
    }

    @Test
    void rejectsSecondPendingRequestForSameMemberAndBook() throws Exception {
        createRequest("member-1", "api-request-2");

        mockMvc.perform(post("/api/v1/borrow-requests")
                        .header(USER_HEADER, "member-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "bookId": 10,
                                  "bookType": "PHYSICAL",
                                  "idempotencyKey": "api-request-3"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("A pending borrow request already exists for this book"));
    }

    private void createRequest(String memberId, String idempotencyKey) throws Exception {
        mockMvc.perform(post("/api/v1/borrow-requests")
                        .header(USER_HEADER, memberId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "bookId": 10,
                                  "bookType": "PHYSICAL",
                                  "idempotencyKey": "%s"
                                }
                                """.formatted(idempotencyKey)))
                .andExpect(status().isCreated());
    }
}
