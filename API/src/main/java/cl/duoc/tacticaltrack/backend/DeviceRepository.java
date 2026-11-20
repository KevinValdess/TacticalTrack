package cl.duoc.tacticaltrack.backend;
import java.util.List;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
public interface DeviceRepository extends JpaRepository<SupervisorDevice,String> {
 List<SupervisorDevice> findBySupervisorIdAndActiveTrue(String supervisorId);
 @Modifying @Query("update SupervisorDevice d set d.active=false where d.installationId=:id and d.fcmToken=:token")
 void disableToken(@Param("id") String id,@Param("token") String token);
}
