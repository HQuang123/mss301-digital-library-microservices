package fu.edu.mss301.digilib.loan.application.usecase;

import fu.edu.mss301.digilib.loan.api.dto.BorrowRequestResponse;
import fu.edu.mss301.digilib.loan.api.dto.CreateBorrowRequest;
import fu.edu.mss301.digilib.loan.application.command.BorrowBookCommand;
import fu.edu.mss301.digilib.loan.domain.aggregate.Loan;
import fu.edu.mss301.digilib.loan.domain.entity.BorrowRequest;
import fu.edu.mss301.digilib.loan.domain.repository.BorrowRequestRepository;
import fu.edu.mss301.digilib.loan.domain.vo.BorrowRequestStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BorrowRequestUseCaseTest {

    @Mock
    private BorrowRequestRepository requestRepository;

    @Mock
    private BorrowBookUseCase borrowBookUseCase;

    private BorrowRequestUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new BorrowRequestUseCase(requestRepository, borrowBookUseCase);
    }

    @Test
    void createsPendingRequestForAuthenticatedMember() {
        CreateBorrowRequest command = new CreateBorrowRequest(10L, "PHYSICAL", "request-1");
        when(requestRepository.findByIdempotencyKey("request-1")).thenReturn(Optional.empty());
        when(requestRepository.existsByMemberIdAndBookIdAndStatus(
                "member-1", 10L, BorrowRequestStatus.PENDING)).thenReturn(false);
        when(requestRepository.saveAndFlush(any(BorrowRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BorrowRequestResponse response = useCase.create("member-1", command);

        assertEquals("member-1", response.memberId());
        assertEquals("PENDING", response.status());
    }

    @Test
    void rejectsSecondPendingRequestForTheSameBook() {
        CreateBorrowRequest command = new CreateBorrowRequest(10L, "PHYSICAL", "request-2");
        when(requestRepository.findByIdempotencyKey("request-2")).thenReturn(Optional.empty());
        when(requestRepository.existsByMemberIdAndBookIdAndStatus(
                "member-1", 10L, BorrowRequestStatus.PENDING)).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> useCase.create("member-1", command));
    }

    @Test
    void approvalDelegatesToExistingBorrowWorkflow() {
        BorrowRequest request = BorrowRequest.create("member-1", 10L, "DIGITAL", "request-3");
        Loan loan = Loan.create(
                "member-1", 10L, null, "DIGITAL",
                LocalDateTime.now().plusDays(14), "borrow-request-null");
        when(requestRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(request));
        when(borrowBookUseCase.handle(any(BorrowBookCommand.class))).thenReturn(loan);
        when(requestRepository.saveAndFlush(request)).thenReturn(request);

        useCase.approve(7L, "librarian-1");

        ArgumentCaptor<BorrowBookCommand> command = ArgumentCaptor.forClass(BorrowBookCommand.class);
        verify(borrowBookUseCase).handle(command.capture());
        assertEquals("member-1", command.getValue().memberId());
        assertEquals(10L, command.getValue().bookId());
        assertEquals(BorrowRequestStatus.APPROVED, request.getStatus());
    }
}
