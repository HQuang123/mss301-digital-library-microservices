package fu.edu.mss301.digilib.loan.application.usecase;

import fu.edu.mss301.digilib.loan.api.dto.BorrowRequestResponse;
import fu.edu.mss301.digilib.loan.api.dto.CreateBorrowRequest;
import fu.edu.mss301.digilib.loan.api.dto.LoanResponse;
import fu.edu.mss301.digilib.loan.application.command.BorrowBookCommand;
import fu.edu.mss301.digilib.loan.domain.aggregate.Loan;
import fu.edu.mss301.digilib.loan.domain.entity.BorrowRequest;
import fu.edu.mss301.digilib.loan.domain.repository.BorrowRequestRepository;
import fu.edu.mss301.digilib.loan.domain.vo.BorrowRequestStatus;
import fu.edu.mss301.digilib.loan.domain.vo.LoanStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class BorrowRequestUseCase {

    private final BorrowRequestRepository requestRepository;
    private final BorrowBookUseCase borrowBookUseCase;

    @Transactional
    public BorrowRequestResponse create(String memberId, CreateBorrowRequest command) {
        return requestRepository.findByIdempotencyKey(command.idempotencyKey())
                .map(existing -> ensureIdempotentOwner(existing, memberId))
                .map(BorrowRequestResponse::from)
                .orElseGet(() -> createNew(memberId, command));
    }

    @Transactional(readOnly = true)
    public Page<BorrowRequestResponse> findMine(String memberId, Pageable pageable) {
        requireAuthenticatedUser(memberId);
        return requestRepository.findByMemberIdOrderByRequestedAtDesc(memberId, pageable)
                .map(BorrowRequestResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<BorrowRequestResponse> findByStatus(String status, Pageable pageable) {
        if (status == null || status.isBlank()) {
            return requestRepository.findAll(pageable).map(BorrowRequestResponse::from);
        }

        String normalizedStatus = status.toUpperCase(Locale.ROOT);
        try {
            BorrowRequestStatus requestStatus = BorrowRequestStatus.valueOf(normalizedStatus);
            return requestRepository.findByStatusOrderByRequestedAtDesc(requestStatus, pageable)
                    .map(BorrowRequestResponse::from);
        } catch (IllegalArgumentException ignored) {
            try {
                LoanStatus loanStatus = LoanStatus.valueOf(normalizedStatus);
                return requestRepository.findApprovedByLoanStatus(loanStatus, pageable)
                        .map(BorrowRequestResponse::from);
            } catch (IllegalArgumentException invalidLoanStatus) {
                throw new IllegalArgumentException("Trạng thái yêu cầu mượn không được hỗ trợ: " + status);
            }
        }
    }

    @Transactional
    public LoanResponse approve(Long requestId, String actorId) {
        BorrowRequest request = findPendingForUpdate(requestId);
        Loan loan = borrowBookUseCase.handle(new BorrowBookCommand(
                request.getMemberId(),
                request.getBookId(),
                request.getBookType(),
                "borrow-request-" + request.getRequestId()));
        request.approve(loan, actorId);
        requestRepository.saveAndFlush(request);
        return LoanResponse.from(loan);
    }

    @Transactional
    public BorrowRequestResponse reject(Long requestId, String reason, String actorId) {
        BorrowRequest request = findPendingForUpdate(requestId);
        request.reject(reason, actorId);
        return BorrowRequestResponse.from(requestRepository.saveAndFlush(request));
    }

    @Transactional
    public void cancel(Long requestId, String memberId) {
        requireAuthenticatedUser(memberId);
        BorrowRequest request = findPendingForUpdate(requestId);
        if (!request.getMemberId().equals(memberId)) {
            throw new IllegalStateException("Thành viên chỉ có thể hủy yêu cầu mượn của chính mình");
        }
        request.cancel(memberId);
        requestRepository.saveAndFlush(request);
    }

    private BorrowRequestResponse createNew(String memberId, CreateBorrowRequest command) {
        requireAuthenticatedUser(memberId);
        if (requestRepository.existsByMemberIdAndBookIdAndStatus(
                memberId, command.bookId(), BorrowRequestStatus.PENDING)) {
            throw new IllegalStateException("Bạn đã có yêu cầu mượn sách này đang chờ duyệt");
        }
        // Re-check on the server immediately before creating PENDING so the
        // frontend eligibility check cannot be bypassed or become authoritative.
        borrowBookUseCase.checkEligibility(memberId);
        try {
            BorrowRequest request = BorrowRequest.create(
                    memberId, command.bookId(), command.bookType(), command.idempotencyKey());
            return BorrowRequestResponse.from(requestRepository.saveAndFlush(request));
        } catch (DataIntegrityViolationException exception) {
            throw new IllegalStateException("Yêu cầu mượn đang chờ duyệt hoặc yêu cầu trùng lặp đã tồn tại");
        }
    }

    private BorrowRequest ensureIdempotentOwner(BorrowRequest request, String memberId) {
        requireAuthenticatedUser(memberId);
        if (!request.getMemberId().equals(memberId)) {
            throw new IllegalStateException("Mã chống trùng lặp đã được sử dụng bởi thành viên khác");
        }
        return request;
    }

    private BorrowRequest findPendingForUpdate(Long requestId) {
        if (requestId == null) {
            throw new IllegalArgumentException("Thiếu mã yêu cầu mượn");
        }
        return requestRepository.findByIdForUpdate(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy yêu cầu mượn: " + requestId));
    }

    private void requireAuthenticatedUser(String memberId) {
        if (memberId == null || memberId.isBlank()) {
            throw new IllegalArgumentException("Không xác định được thành viên đang đăng nhập");
        }
    }
}
