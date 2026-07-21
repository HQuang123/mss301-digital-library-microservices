package fu.edu.mss301.digilib.loan.domain.entity;

import fu.edu.mss301.digilib.loan.domain.aggregate.Loan;
import fu.edu.mss301.digilib.loan.domain.vo.BorrowRequestStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BorrowRequestTest {

    @Test
    void createsPendingRequestWithNormalizedBookType() {
        BorrowRequest request = BorrowRequest.create("member-1", 10L, "physical", "request-1");

        assertEquals(BorrowRequestStatus.PENDING, request.getStatus());
        assertEquals("PHYSICAL", request.getBookType());
        assertNotNull(request.getRequestedAt());
    }

    @Test
    void approvingRequestLinksTheCreatedLoan() {
        BorrowRequest request = BorrowRequest.create("member-1", 10L, "DIGITAL", "request-2");
        Loan loan = Loan.create(
                "member-1", 10L, null, "DIGITAL",
                LocalDateTime.now().plusDays(14), "borrow-request-2");

        request.approve(loan, "librarian-1");

        assertEquals(BorrowRequestStatus.APPROVED, request.getStatus());
        assertEquals(loan, request.getLoan());
        assertEquals("librarian-1", request.getProcessedBy());
        assertNotNull(request.getProcessedAt());
    }

    @Test
    void processedRequestCannotBeProcessedAgain() {
        BorrowRequest request = BorrowRequest.create("member-1", 10L, "DIGITAL", "request-3");
        request.reject("Not eligible", "librarian-1");

        assertThrows(IllegalStateException.class, () -> request.cancel("member-1"));
    }

    @Test
    void rejectsUnsupportedBookType() {
        assertThrows(IllegalArgumentException.class,
                () -> BorrowRequest.create("member-1", 10L, "AUDIO", "request-4"));
    }
}
