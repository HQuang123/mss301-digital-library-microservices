package fu.edu.mss301.digilib.loan.application.usecase;

import fu.edu.mss301.digilib.loan.domain.aggregate.Loan;
import fu.edu.mss301.digilib.loan.domain.repository.LoanRepository;
import fu.edu.mss301.digilib.loan.domain.vo.LoanStatus;
import fu.edu.mss301.digilib.loan.infrastructure.adapter.BookCatalogClientAdapter;
import fu.edu.mss301.digilib.loan.infrastructure.adapter.MemberClientAdapter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ManageBorrowRequestUseCase {

    private final LoanRepository loanRepository;
    private final BorrowBookUseCase borrowBookUseCase;
    private final MemberClientAdapter memberClient;
    private final BookCatalogClientAdapter catalogClient;

    @Transactional
    public Loan create(String memberId, Long bookId, String bookType, String idempotencyKey) {
        return loanRepository.findByIdempotencyKey(idempotencyKey).orElseGet(() -> {
            if (loanRepository.existsByMemberIdAndBookIdAndStatus(memberId, bookId, LoanStatus.PENDING)) {
                throw new IllegalStateException("A pending borrow request already exists for this book");
            }
            memberClient.getMember(memberId);
            catalogClient.getBookDetails(bookId);
            return loanRepository.save(Loan.request(memberId, bookId, bookType, idempotencyKey));
        });
    }

    @Transactional(readOnly = true)
    public Page<Loan> findMine(String memberId, Pageable pageable) {
        return loanRepository.findBorrowRequestsByMember(memberId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Loan> findForReview(LoanStatus status, Pageable pageable) {
        return loanRepository.findBorrowRequestsByStatus(
                status == null ? LoanStatus.PENDING : status, pageable);
    }

    @Transactional
    public Loan approve(Long requestId, String reviewerId) {
        return borrowBookUseCase.approve(findPending(requestId), reviewerId);
    }

    @Transactional
    public Loan reject(Long requestId, String reviewerId, String reason) {
        Loan request = findPending(requestId);
        request.reject(reviewerId, reason);
        return loanRepository.save(request);
    }

    @Transactional
    public Loan cancel(Long requestId, String memberId) {
        Loan request = findPending(requestId);
        if (!request.getMemberId().equals(memberId)) {
            throw new IllegalArgumentException("Borrow request does not belong to the authenticated member");
        }
        request.cancel(memberId);
        return loanRepository.save(request);
    }

    private Loan findPending(Long requestId) {
        Loan request = loanRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Borrow request not found: " + requestId));
        if (request.getStatus() != LoanStatus.PENDING) {
            throw new IllegalStateException("Borrow request is no longer pending");
        }
        return request;
    }
}
