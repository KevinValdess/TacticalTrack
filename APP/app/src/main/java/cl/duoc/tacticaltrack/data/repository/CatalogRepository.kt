package cl.duoc.tacticaltrack.data.repository
import cl.duoc.tacticaltrack.data.remote.CatalogApi
import cl.duoc.tacticaltrack.model.PointOfSale
import cl.duoc.tacticaltrack.model.CoolingAsset

data class CatalogSnapshot(val points: List<PointOfSale>,val assets: List<CoolingAsset>)
class CatalogRepository(private val api: CatalogApi) {
 suspend fun load(): CatalogSnapshot {
  val points=api.points();val assets=api.assets()
  require(points.isNotEmpty()) {"No hay puntos disponibles"}
  require(assets.all { a->points.any { it.id==a.pointId } }) {"Catálogo inconsistente"}
  return CatalogSnapshot(points,assets)
 }
}
