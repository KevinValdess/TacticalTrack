package cl.duoc.tacticaltrack
import cl.duoc.tacticaltrack.data.remote.*
import cl.duoc.tacticaltrack.data.repository.*
import cl.duoc.tacticaltrack.model.TestCatalog
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.Assert.*
class CatalogAndDeviceTest {
 @Test fun catalogLoadsRemoteData()=runTest {
  val api=mockk<CatalogApi>();coEvery {api.points()} returns TestCatalog.points;coEvery {api.assets()} returns TestCatalog.assets
  val result=CatalogRepository(api).load();assertEquals(3,result.points.size);assertEquals(5,result.assets.size)
 }
 @Test fun emptyAndInconsistentCatalogsAreRejected()=runTest {
  val api=mockk<CatalogApi>();coEvery {api.points()} returns emptyList();coEvery {api.assets()} returns emptyList()
  try{CatalogRepository(api).load();fail()}catch(_:IllegalArgumentException){}
  coEvery {api.points()} returns TestCatalog.points.take(1);coEvery {api.assets()} returns TestCatalog.assets
  try{CatalogRepository(api).load();fail()}catch(_:IllegalArgumentException){}
 }
 @Test fun registerAndDisableUseDifferentEndpoints()=runTest {
  val api=mockk<DeviceApi>(relaxed=true);val repo=DeviceRegistrationRepository(api)
  repo.sync("installation-123","SUP-001","token",true)
  coVerify {api.register("installation-123",DeviceRegistration("SUP-001","token"))}
  repo.sync("installation-123","SUP-001",null,false);coVerify {api.unregister("installation-123")}
 }
 @Test fun malformedRegistrationDoesNotReachApi()=runTest {
  val api=mockk<DeviceApi>(relaxed=true);val repo=DeviceRegistrationRepository(api)
  try{repo.sync("x","SUP-001","token",true);fail()}catch(_:IllegalArgumentException){}
  try{repo.sync("installation-123","SUP-001",null,true);fail()}catch(_:IllegalArgumentException){}
  try{repo.sync("installation-123","","token",true);fail()}catch(_:IllegalArgumentException){}
  coVerify(exactly=0) {api.register(any(),any())}
 }
}
