package cl.duoc.tacticaltrack.backend;
import jakarta.persistence.*;
@Entity @Table(name="push_outbox")
public class PushOutbox {
 @Id public Long visitId;
 public String supervisorId;
 public String state="PENDING";
 public int attempts;
 public long createdAt;
 public long nextAttemptAt;
 public String lastError;
}
