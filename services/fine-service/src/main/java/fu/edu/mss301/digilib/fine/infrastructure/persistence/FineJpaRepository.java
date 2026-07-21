package fu.edu.mss301.digilib.fine.infrastructure.persistence;

import fu.edu.mss301.digilib.fine.domain.entity.Fine;
import fu.edu.mss301.digilib.fine.domain.vo.FineStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;

public interface FineJpaRepository extends JpaRepository<Fine, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select fine from Fine fine where fine.id = :id")
    Optional<Fine> findByIdForUpdate(@Param("id") Integer id);

    boolean existsByStudentIdAndStatus(String studentId, FineStatus status);

    Optional<Fine> findByLoanId(Long loanId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select fine from Fine fine where fine.loanId = :loanId")
    Optional<Fine> findByLoanIdForUpdate(@Param("loanId") Long loanId);

    Page<Fine> findByStudentId(String studentId, Pageable pageable);

    Page<Fine> findByStudentIdAndStatusIn(String studentId, Collection<FineStatus> statuses, Pageable pageable);

    Page<Fine> findByStatusIn(Collection<FineStatus> statuses, Pageable pageable);
}
