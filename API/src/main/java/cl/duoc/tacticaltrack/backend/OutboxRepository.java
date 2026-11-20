package cl.duoc.tacticaltrack.backend;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
public interface OutboxRepository extends JpaRepository<PushOutbox,Long> {
 List<PushOutbox> findTop20ByStateAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(String state,long now);
}
