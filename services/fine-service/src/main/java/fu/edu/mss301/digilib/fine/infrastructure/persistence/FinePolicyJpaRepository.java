package fu.edu.mss301.digilib.fine.infrastructure.persistence;

import fu.edu.mss301.digilib.fine.domain.entity.FinePolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FinePolicyJpaRepository extends JpaRepository<FinePolicy, Integer> {

    Optional<FinePolicy> findFirstByIsActiveTrueOrderByIdDesc();
}
