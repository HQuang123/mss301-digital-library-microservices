package fu.edu.mss301.digilib.fine.application.service;

import fu.edu.mss301.digilib.fine.api.dto.BorrowEligibilityResponse;
import fu.edu.mss301.digilib.fine.api.dto.FineResponse;
import fu.edu.mss301.digilib.fine.api.dto.LostBookFineRequest;
import fu.edu.mss301.digilib.fine.api.dto.OverdueReturnFineRequest;
import fu.edu.mss301.digilib.fine.api.dto.OverdueThresholdFineRequest;
import fu.edu.mss301.digilib.fine.application.exception.BusinessConflictException;
import fu.edu.mss301.digilib.fine.application.exception.ResourceNotFoundException;
import fu.edu.mss301.digilib.fine.domain.aggregate.FineAggregate;
import fu.edu.mss301.digilib.fine.domain.entity.Fine;
import fu.edu.mss301.digilib.fine.domain.entity.FinePolicy;
import fu.edu.mss301.digilib.fine.domain.vo.FineReason;
import fu.edu.mss301.digilib.fine.domain.vo.FineStatus;
import fu.edu.mss301.digilib.fine.infrastructure.persistence.FineJpaRepository;
import fu.edu.mss301.digilib.fine.infrastructure.persistence.FinePolicyJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Backs the /internal/fines/** endpoints Loan Service calls when it creates,
 * updates, or checks fines (see FLOW.md Flow 1/3/4/5). Every loan may have at
 * most one Fine row (uk_fines_loan_id), so the threshold and lost-book paths
 * update the existing row instead of creating a second one for the same loan.
 */
@Service
public class InternalFineService {

    private final FineJpaRepository fineRepository;
    private final FinePolicyJpaRepository finePolicyRepository;

    public InternalFineService(FineJpaRepository fineRepository, FinePolicyJpaRepository finePolicyRepository) {
        this.fineRepository = fineRepository;
        this.finePolicyRepository = finePolicyRepository;
    }

    @Transactional(readOnly = true)
    public BorrowEligibilityResponse checkBorrowEligibility(String studentId) {
        boolean hasPendingFine = fineRepository.existsByStudentIdAndStatus(studentId, FineStatus.PENDING);
        if (hasPendingFine) {
            return new BorrowEligibilityResponse(false, "Bạn đang có khoản phạt chưa thanh toán");
        }
        return new BorrowEligibilityResponse(true, null);
    }

    @Transactional
    public FineResponse createOverdueReturnFine(OverdueReturnFineRequest request) {
        Long loanId = parseLoanId(request.loanId());
        if (fineRepository.findByLoanIdForUpdate(loanId).isPresent()) {
            throw new BusinessConflictException("A fine already exists for loan " + request.loanId());
        }

        Fine fine = activeAggregate().createFineFor(
                loanId,
                request.studentId(),
                null,
                request.dueDate().atStartOfDay(),
                request.returnDate().atStartOfDay(),
                FineReason.OVERDUE_RETURN,
                0L);
        fine.setBookId(parseBookId(request.bookId()));

        return FineResponse.from(fineRepository.save(fine));
    }

    @Transactional
    public FineResponse createOrUpdateThresholdFine(OverdueThresholdFineRequest request) {
        Long loanId = parseLoanId(request.loanId());
        long compensationAmount = request.compensationEnabled() && request.bookValue() != null
                ? request.bookValue()
                : 0L;

        return fineRepository.findByLoanIdForUpdate(loanId)
                .map(existing -> updateExistingFine(existing, request.overdueDays(), compensationAmount))
                .orElseGet(() -> createThresholdOrLostFine(
                        loanId, request.studentId(), request.bookId(), request.overdueDays(),
                        FineReason.OVERDUE_THRESHOLD, compensationAmount));
    }

    @Transactional
    public FineResponse createOrUpdateLostBookFine(LostBookFineRequest request) {
        Long loanId = parseLoanId(request.loanId());
        long compensationAmount = request.bookValue() != null ? request.bookValue() : 0L;

        return fineRepository.findByLoanIdForUpdate(loanId)
                .map(existing -> {
                    existing.setReason(FineReason.LOST_BOOK);
                    return updateExistingFine(existing, request.overdueDays(), compensationAmount);
                })
                .orElseGet(() -> createThresholdOrLostFine(
                        loanId, request.studentId(), request.bookId(), request.overdueDays(),
                        FineReason.LOST_BOOK, compensationAmount));
    }

    private FineResponse updateExistingFine(Fine existing, long overdueDays, long compensationAmount) {
        if (existing.getStatus() != FineStatus.PENDING) {
            throw new BusinessConflictException(
                    "Fine for loan " + existing.getLoanId() + " is already " + existing.getStatus()
                            + " and cannot be updated");
        }

        FineAggregate aggregate = FineAggregate.create(existing.getFinePolicy());
        existing.setDueDate(LocalDateTime.now().minusDays(overdueDays));
        aggregate.recalculate(existing, null, compensationAmount);

        return FineResponse.from(fineRepository.save(existing));
    }

    private FineResponse createThresholdOrLostFine(
            Long loanId,
            String studentId,
            String bookId,
            long overdueDays,
            FineReason reason,
            long compensationAmount
    ) {
        LocalDateTime syntheticDueDate = LocalDateTime.now().minusDays(overdueDays);
        Fine fine = activeAggregate().createFineFor(
                loanId, studentId, null, syntheticDueDate, null, reason, compensationAmount);
        fine.setBookId(parseBookId(bookId));

        return FineResponse.from(fineRepository.save(fine));
    }

    private FineAggregate activeAggregate() {
        FinePolicy policy = finePolicyRepository.findFirstByIsActiveTrueOrderByIdDesc()
                .orElseThrow(() -> new ResourceNotFoundException("No active fine policy is configured"));
        return FineAggregate.create(policy);
    }

    private Long parseLoanId(String loanId) {
        try {
            return Long.valueOf(loanId);
        } catch (NumberFormatException exception) {
            throw new BusinessConflictException("loanId must be numeric: " + loanId);
        }
    }

    private Long parseBookId(String bookId) {
        if (bookId == null || bookId.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(bookId);
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
