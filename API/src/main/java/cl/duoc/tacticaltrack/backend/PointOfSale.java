package cl.duoc.tacticaltrack.backend;
import jakarta.persistence.*;
@Entity @Table(name="points_of_sale")
public class PointOfSale {
 @Id public String id;
 public String name;
 public String address;
 public double latitude;
 public double longitude;
 public String supervisorId;
 public PointOfSale() {}
 public PointOfSale(String id,String name,String address,double latitude,double longitude,String supervisorId) {
  this.id=id;this.name=name;this.address=address;this.latitude=latitude;this.longitude=longitude;this.supervisorId=supervisorId;
 }
}
