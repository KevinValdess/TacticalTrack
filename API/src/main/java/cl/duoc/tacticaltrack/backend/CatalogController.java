package cl.duoc.tacticaltrack.backend;
import java.util.List;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api")
public class CatalogController {
 private final CatalogService catalog;
 public CatalogController(CatalogService catalog){this.catalog=catalog;}
 @GetMapping("/points") public List<PointOfSale> points(){return catalog.points();}
 @GetMapping("/assets") public List<CoolingAsset> assets(@RequestParam(required=false) String pointId){return catalog.assets(pointId);}
}
