package cl.duoc.tacticaltrack.backend;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
@Service
public class CatalogService {
 private final PointRepository points; private final AssetRepository assets;
 public CatalogService(PointRepository points,AssetRepository assets){this.points=points;this.assets=assets;}
 public List<PointOfSale> points(){return points.findAll();}
 public List<CoolingAsset> assets(String pointId){return pointId==null?assets.findAll():assets.findByPointId(pointId);}
 public PointOfSale point(String id){return points.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.BAD_REQUEST,"Punto inválido"));}
 public CoolingAsset asset(String pointId,String id){
  var a=assets.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.BAD_REQUEST,"Equipo inválido"));
  if(!a.pointId.equals(pointId))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"El equipo no pertenece al punto");
  return a;
 }
 public boolean supervisorExists(String id){return points.findAll().stream().anyMatch(p->id.equals(p.supervisorId));}
}
