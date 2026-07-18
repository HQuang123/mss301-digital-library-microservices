package fu.edu.mss301.digilib.loan.domain.repository;

import fu.edu.mss301.digilib.loan.domain.aggregate.Loan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.time.LocalDateTime;
import java.util.List;
import fu.edu.mss301.digilib.loan.domain.vo.LoanStatus;

public interface LoanRepository extends JpaRepository<Loan, Long> {
    Optional<Loan> findByIdempotencyKey(String idempotencyKey);
    List<Loan> findByMemberIdOrderByBorrowedAtDesc(String memberId);
    List<Loan> findByStatusAndDueDateBefore(LoanStatus status, LocalDateTime dueDate);
    List<Loan> findByStatusInAndDueDateBefore(List<LoanStatus> statuses, LocalDateTime dueDate);
    long countByMemberIdAndStatusIn(String memberId, List<LoanStatus> statuses);
}
