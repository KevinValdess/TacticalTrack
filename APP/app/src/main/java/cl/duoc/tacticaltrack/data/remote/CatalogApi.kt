package cl.duoc.tacticaltrack.data.remote
import cl.duoc.tacticaltrack.model.PointOfSale
import cl.duoc.tacticaltrack.model.CoolingAsset
import retrofit2.http.GET
interface CatalogApi {
 @GET("api/points") suspend fun points(): List<PointOfSale>
 @GET("api/assets") suspend fun assets(): List<CoolingAsset>
}
