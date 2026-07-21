package fu.edu.mss301.digilib.fine.application.service;

import fu.edu.mss301.digilib.fine.api.dto.FineResponse;
import fu.edu.mss301.digilib.fine.api.dto.PaymentAttemptResponse;
import fu.edu.mss301.digilib.fine.application.exception.BusinessConflictException;
import fu.edu.mss301.digilib.fine.application.exception.ResourceNotFoundException;
import fu.edu.mss301.digilib.fine.domain.aggregate.FineAggregate;
import fu.edu.mss301.digilib.fine.domain.entity.Fine;
import fu.edu.mss301.digilib.fine.domain.vo.FineStatus;
import fu.edu.mss301.digilib.fine.infrastructure.client.CatalogServiceClient;
import fu.edu.mss301.digilib.fine.infrastructure.client.dto.BookSummaryDto;
import fu.edu.mss301.digilib.fine.infrastructure.persistence.FineJpaRepository;
import fu.edu.mss301.digilib.fine.infrastructure.persistence.PaymentAttemptJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Backs Flow 7 (FINE_HISTORY_FLOW.md): both the student "My Fines" screen and
 * the librarian "Member Fine Lookup" screen call the same read/management
 * operations here — the two controllers differ only in which studentId they
 * are allowed to pass and whether they expose the waive/mark-paid actions.
 */
@Service
public class FineHistoryService {

    private final FineJpaRepository fineRepository;
    private final PaymentAttemptJpaRepository paymentRepository;
    private final CatalogServiceClient catalogServiceClient;

    public FineHistoryService(
            FineJpaRepository fineRepository,
            PaymentAttemptJpaRepository paymentRepository,
            CatalogServiceClient catalogServiceClient
    ) {
        this.fineRepository = fineRepository;
        this.paymentRepository = paymentRepository;
        this.catalogServiceClient = catalogServiceClient;
    }

    @Transactional(readOnly = true)
    public Page<FineResponse> getStudentFines(String studentId, List<FineStatus> statuses, Pageable pageable) {
        Page<Fine> fines = (statuses == null || statuses.isEmpty())
                ? fineRepository.findByStudentId(studentId, pageable)
                : fineRepository.findByStudentIdAndStatusIn(studentId, statuses, pageable);

        return fines.map(this::toEnrichedResponse);
    }

    @Transactional(readOnly = true)
    public Page<FineResponse> getAllFines(List<FineStatus> statuses, Pageable pageable) {
        Page<Fine> fines = (statuses == null || statuses.isEmpty())
                ? fineRepository.findAll(pageable)
                : fineRepository.findByStatusIn(statuses, pageable);

        return fines.map(this::toEnrichedResponse);
    }

    @Transactional(readOnly = true)
    public String getFineStudentId(Integer fineId) {
        return fineRepository.findById(fineId)
                .map(Fine::getStudentId)
                .orElseThrow(() -> new ResourceNotFoundException("Fine " + fineId + " was not found"));
    }

    @Transactional(readOnly = true)
    public List<PaymentAttemptResponse> getPayments(Integer fineId) {
        if (!fineRepository.existsById(fineId)) {
            throw new ResourceNotFoundException("Fine " + fineId + " was not found");
        }

        return paymentRepository.findByFine_IdOrderByCreatedAtDesc(fineId).stream()
                .map(PaymentAttemptResponse::from)
                .toList();
    }

    @Transactional
    public FineResponse waiveFine(Integer fineId, String waiverReason) {
        Fine fine = fineRepository.findByIdForUpdate(fineId)
                .orElseThrow(() -> new ResourceNotFoundException("Fine " + fineId + " was not found"));

        FineAggregate.create(fine.getFinePolicy()).waive(fine, waiverReason);

        return FineResponse.from(fineRepository.save(fine));
    }

    @Transactional
    public FineResponse markFinePaid(Integer fineId) {
        Fine fine = fineRepository.findByIdForUpdate(fineId)
                .orElseThrow(() -> new ResourceNotFoundException("Fine " + fineId + " was not found"));

        if (fine.getStatus() != FineStatus.PENDING) {
            throw new BusinessConflictException("Only a pending fine can be marked paid");
        }

        FineAggregate.create(fine.getFinePolicy()).markPaid(fine);

        return FineResponse.from(fineRepository.save(fine));
    }

    private FineResponse toEnrichedResponse(Fine fine) {
        return FineResponse.from(fine, resolveBookTitle(fine.getBookId()));
    }

    private String resolveBookTitle(Long bookId) {
        if (bookId == null) {
            return null;
        }

        try {
            BookSummaryDto book = catalogServiceClient.getBook(bookId);
            return book != null ? book.title() : null;
        } catch (RuntimeException exception) {
            // Best-effort enrichment: Catalog Service being down or the book
            // being deleted should not break the fine history screen.
            return null;
        }
    }
}
