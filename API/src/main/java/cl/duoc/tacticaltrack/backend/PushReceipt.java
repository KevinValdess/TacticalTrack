package cl.duoc.tacticaltrack.backend;
import jakarta.persistence.*;
@Entity @Table(name="push_receipts")
public class PushReceipt {
 @Id public String id;
 public Long visitId;
 public String installationId;
 public String result;
 public long processedAt;
}
