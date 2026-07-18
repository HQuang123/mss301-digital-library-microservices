package fu.edu.mss301.digilib.loan.domain.aggregate;

import fu.edu.mss301.digilib.loan.domain.vo.LoanStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class LoanTest {

    @Test
    void createsActiveLoanWithRequiredDefaults() {
        Loan loan = Loan.create("member-1", 2L, 3L, "physical",
                LocalDateTime.now().plusDays(14), "borrow-1");

        assertEquals(LoanStatus.BORROWED, loan.getStatus());
        assertEquals(0, loan.getRenewalCount());
        assertEquals(3L, loan.getCopyId());
        assertEquals("PHYSICAL", loan.getBookType());
        assertEquals(1, loan.getHistories().size());
    }

    @Test
    void returningTwiceIsRejected() {
        Loan loan = Loan.create("member-1", 2L, 3L, "PHYSICAL",
                LocalDateTime.now().plusDays(14), "borrow-2");

        loan.returnBook("librarian-1");

        assertEquals(LoanStatus.RETURNED, loan.getStatus());
        assertNotNull(loan.getReturnedAt());
        assertThrows(IllegalStateException.class, () -> loan.returnBook("librarian-1"));
    }

    @Test
    void returnedLoanCannotBeRenewed() {
        Loan loan = Loan.create("member-1", 2L, 3L, "PHYSICAL",
                LocalDateTime.now().plusDays(14), "borrow-3");
        loan.returnBook("librarian-1");

        assertThrows(IllegalStateException.class, () -> loan.renew("member-1"));
    }

    @Test
    void activeLoanCanBeMarkedLost() {
        Loan loan = Loan.create("member-1", 2L, 3L, "PHYSICAL",
                LocalDateTime.now().plusDays(14), "borrow-4");

        loan.markLost("librarian-1");

        assertEquals(LoanStatus.LOST, loan.getStatus());
        assertEquals(LoanStatus.LOST, loan.getHistories().getLast().getToStatus());
    }

    @Test
    void lostLoanCannotBeReturnedOrMarkedLostAgain() {
        Loan loan = Loan.create("member-1", 2L, 3L, "PHYSICAL",
                LocalDateTime.now().plusDays(14), "borrow-5");
        loan.markLost("librarian-1");

        assertThrows(IllegalStateException.class, () -> loan.returnBook("librarian-1"));
        assertThrows(IllegalStateException.class, () -> loan.markLost("librarian-1"));
    }
}
