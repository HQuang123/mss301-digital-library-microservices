package fu.edu.mss301.digilib.loan.domain.repository;

import fu.edu.mss301.digilib.loan.domain.entity.BorrowRequest;
import fu.edu.mss301.digilib.loan.domain.vo.BorrowRequestStatus;
import fu.edu.mss301.digilib.loan.domain.vo.LoanStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface BorrowRequestRepository extends JpaRepository<BorrowRequest, Long> {

    Optional<BorrowRequest> findByIdempotencyKey(String idempotencyKey);

    boolean existsByMemberIdAndBookIdAndStatus(String memberId, Long bookId, BorrowRequestStatus status);

    Page<BorrowRequest> findByMemberIdOrderByRequestedAtDesc(String memberId, Pageable pageable);

    Page<BorrowRequest> findByStatusOrderByRequestedAtDesc(BorrowRequestStatus status, Pageable pageable);

    @Query("""
            select request from BorrowRequest request
            join request.loan loan
            where request.status = fu.edu.mss301.digilib.loan.domain.vo.BorrowRequestStatus.APPROVED
              and loan.status = :loanStatus
            order by request.requestedAt desc
            """)
    Page<BorrowRequest> findApprovedByLoanStatus(
            @Param("loanStatus") LoanStatus loanStatus,
            Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select request from BorrowRequest request where request.requestId = :requestId")
    Optional<BorrowRequest> findByIdForUpdate(@Param("requestId") Long requestId);
}
