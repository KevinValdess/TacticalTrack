package cl.duoc.tacticaltrack.backend;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
public interface AssetRepository extends JpaRepository<CoolingAsset,String> { List<CoolingAsset> findByPointId(String pointId); }
