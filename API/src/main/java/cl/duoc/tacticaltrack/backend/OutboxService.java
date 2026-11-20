package cl.duoc.tacticaltrack.backend;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service @Transactional
public class OutboxService {
 private final OutboxRepository jobs;private final ReceiptRepository receipts;
 public OutboxService(OutboxRepository jobs,ReceiptRepository receipts){this.jobs=jobs;this.receipts=receipts;}
 public void enqueue(Visit visit){
  if(jobs.existsById(visit.id))return;
  var job=new PushOutbox();job.visitId=visit.id;job.supervisorId=visit.supervisorId;
  job.createdAt=System.currentTimeMillis();job.nextAttemptAt=job.createdAt;jobs.save(job);
 }
 public void cancel(long visitId){jobs.deleteById(visitId);receipts.deleteByVisitId(visitId);}
}
