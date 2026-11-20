package cl.duoc.tacticaltrack.backend;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:outboxtest;DB_CLOSE_DELAY=-1","spring.jpa.hibernate.ddl-auto=create-drop","push.enabled=false"})
class OutboxPersistenceTest {
 @Autowired VisitService visits;@Autowired OutboxRepository jobs;@Autowired VisitJpaRepository repository;@Autowired CatalogService catalog;
 @Test void visitAndOutboxAreCommittedOnceWithoutFirebase(){
  var v=VisitServiceTest.valid();v.clientRequestId="outbox-success";v=visits.create(v);
  assertEquals("SUP-001",v.supervisorId);assertEquals("PENDING",jobs.findById(v.id).orElseThrow().state);
  var retry=VisitServiceTest.valid();retry.clientRequestId="outbox-success";
  assertEquals(v.id,visits.create(retry).id);assertEquals(1,jobs.count());
  visits.delete(v.id);assertFalse(jobs.existsById(v.id));
 }
 @Test void invalidAssetCannotCommitVisitOrNotice(){
  var v=VisitServiceTest.valid();v.clientRequestId="invalid-asset";v.assetId="ACT-003";
  long before=repository.count();long notices=jobs.count();
  assertThrows(Exception.class,()->visits.create(v));assertEquals(before,repository.count());assertEquals(notices,jobs.count());
 }
 @Test void catalogHasAssignedSupervisorAndAssets(){
  assertEquals(3,catalog.points().size());assertEquals(2,catalog.assets("PV-001").size());
  assertThrows(Exception.class,()->catalog.asset("PV-001","ACT-003"));
 }
}
