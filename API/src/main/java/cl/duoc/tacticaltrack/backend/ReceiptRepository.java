package cl.duoc.tacticaltrack.backend;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ReceiptRepository extends JpaRepository<PushReceipt,String> { void deleteByVisitId(long visitId); }
