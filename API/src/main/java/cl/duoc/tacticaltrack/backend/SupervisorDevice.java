package cl.duoc.tacticaltrack.backend;
import jakarta.persistence.*;
@Entity @Table(name="supervisor_devices")
public class SupervisorDevice {
 @Id public String installationId;
 public String supervisorId;
 @Column(length=4096) public String fcmToken;
 public boolean active;
 public long updatedAt;
}
