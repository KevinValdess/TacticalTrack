package cl.duoc.tacticaltrack.backend;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
@Component
public class CatalogSeed implements CommandLineRunner {
 private final PointRepository points;private final AssetRepository assets;
 public CatalogSeed(PointRepository points,AssetRepository assets){this.points=points;this.assets=assets;}
 public void run(String...args){
  if(points.count()==0)points.saveAll(List.of(
   new PointOfSale("PV-001","Punto Central","Sucursal 001",-33.5186,-70.7159,"SUP-001"),
   new PointOfSale("PV-002","Punto Norte","Sucursal 002",-33.4372,-70.6506,"SUP-001"),
   new PointOfSale("PV-003","Punto Sur","Sucursal 003",-33.5592,-70.6751,"SUP-001")));
  if(assets.count()==0)assets.saveAll(List.of(
   new CoolingAsset("ACT-001","PV-001","FRZ-001","Vitrina freezer"),new CoolingAsset("ACT-002","PV-001","CON-002","Conservadora"),
   new CoolingAsset("ACT-003","PV-002","FRZ-003","Vitrina freezer"),new CoolingAsset("ACT-004","PV-002","CON-004","Conservadora"),
   new CoolingAsset("ACT-005","PV-003","FRZ-005","Vitrina freezer")));
 }
}
