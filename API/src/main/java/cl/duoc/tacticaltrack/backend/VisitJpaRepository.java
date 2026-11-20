package cl.duoc.tacticaltrack.backend;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VisitJpaRepository extends JpaRepository<Visit, Long> {
    List<Visit> findAllByOrderByTimestampDesc();
    Optional<Visit> findByClientRequestId(String clientRequestId);
}
