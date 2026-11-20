package cl.duoc.tacticaltrack.backend;
import jakarta.persistence.*;
@Entity @Table(name="cooling_assets")
public class CoolingAsset {
 @Id public String id;
 public String pointId;
 public String code;
 public String type;
 public CoolingAsset() {}
 public CoolingAsset(String id,String pointId,String code,String type) {this.id=id;this.pointId=pointId;this.code=code;this.type=type;}
}
