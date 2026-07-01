package fu.edu.mss301.digilib.fine.infrastructure.persistence;

import fu.edu.mss301.digilib.fine.domain.entity.Fine;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface FineJpaRepository extends JpaRepository<Fine, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select fine from Fine fine where fine.id = :id")
    Optional<Fine> findByIdForUpdate(@Param("id") Integer id);
}
