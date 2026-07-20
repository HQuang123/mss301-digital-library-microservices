package fu.edu.mss301.digilib.loan.domain.repository;

import fu.edu.mss301.digilib.loan.domain.aggregate.Loan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import fu.edu.mss301.digilib.loan.domain.vo.LoanStatus;

import java.util.Optional;
import java.time.LocalDateTime;
import java.util.List;
import fu.edu.mss301.digilib.loan.domain.vo.LoanStatus;

public interface LoanRepository extends JpaRepository<Loan, Long> {

    Optional<Loan> findByIdempotencyKey(String idempotencyKey);

    List<Loan> findByMemberIdOrderByBorrowedAtDesc(String memberId);

    List<Loan> findByMemberIdAndStatusInOrderByBorrowedAtDesc(String memberId, List<LoanStatus> statuses);

    @Query("SELECT l FROM Loan l WHERE l.memberId = :memberId " +
            "AND (l.reviewedAt IS NOT NULL OR l.status = LoanStatus.PENDING) " +
            "ORDER BY l.createdAt DESC")
    Page<Loan> findBorrowRequestsByMember(@Param("memberId") String memberId, Pageable pageable);

    @Query("SELECT l FROM Loan l WHERE l.status = :status " +
            "AND (l.reviewedAt IS NOT NULL OR l.status = LoanStatus.PENDING) " +
            "ORDER BY l.createdAt ASC")
    Page<Loan> findBorrowRequestsByStatus(@Param("status") LoanStatus status, Pageable pageable);

    boolean existsByMemberIdAndBookIdAndStatus(String memberId, Long bookId, LoanStatus status);

    Page<Loan> findByStatusIn(List<LoanStatus> statuses, Pageable pageable);

    List<Loan> findByStatusAndDueDateBefore(LoanStatus status, LocalDateTime dueDate);

    List<Loan> findByStatusInAndDueDateBefore(List<LoanStatus> statuses, LocalDateTime dueDate);

    List<Loan> findByStatusInAndDueDateBetween(List<LoanStatus> statuses, LocalDateTime from, LocalDateTime to);

    long countByMemberIdAndStatusIn(String memberId, List<LoanStatus> statuses);
}
