package fu.edu.mss301.digilib.fine.infrastructure.persistence;

import fu.edu.mss301.digilib.fine.domain.entity.PaymentAttempt;
import fu.edu.mss301.digilib.fine.domain.vo.PaymentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;

public interface PaymentAttemptJpaRepository extends JpaRepository<PaymentAttempt, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select payment
            from PaymentAttempt payment
            join fetch payment.fine
            where payment.paymentCode = :paymentCode
            """)
    Optional<PaymentAttempt> findByPaymentCodeForUpdate(@Param("paymentCode") String paymentCode);

    Optional<PaymentAttempt> findBySepayTransactionId(String sepayTransactionId);

    Optional<PaymentAttempt> findFirstByFine_IdOrderByCreatedAtDesc(Integer fineId);

    @Modifying
    @Query("""
            update PaymentAttempt payment
            set payment.status = :expiredStatus,
                payment.updatedAt = :updatedAt
            where payment.fine.id = :fineId
              and payment.status in :activeStatuses
            """)
    int expireActiveAttempts(
            @Param("fineId") Integer fineId,
            @Param("activeStatuses") Collection<PaymentStatus> activeStatuses,
            @Param("expiredStatus") PaymentStatus expiredStatus,
            @Param("updatedAt") LocalDateTime updatedAt
    );
}
