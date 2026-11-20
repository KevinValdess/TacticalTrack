package cl.duoc.tacticaltrack.backend;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
@Component @ConditionalOnProperty(name="push.enabled",havingValue="true")
public class PushWorker {
 private final OutboxRepository jobs;private final ReceiptRepository receipts;private final DeviceRepository devices;private final PushSender sender;
 public PushWorker(OutboxRepository jobs,ReceiptRepository receipts,DeviceRepository devices,PushSender sender){
  this.jobs=jobs;this.receipts=receipts;this.devices=devices;this.sender=sender;
 }
 @Scheduled(fixedDelayString="${push.poll-ms:15000}") @Transactional
 public void poll(){
  long now=System.currentTimeMillis();
  for(var job:jobs.findTop20ByStateAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc("PENDING",now))process(job,now);
 }
 void process(PushOutbox job,long now){
  if(now-job.createdAt>86400000L){job.state="EXPIRED";jobs.save(job);return;}
  if(job.attempts>=8){job.state="FAILED";jobs.save(job);return;}
  var targets=devices.findBySupervisorIdAndActiveTrue(job.supervisorId);
  if(targets.isEmpty()){job.nextAttemptAt=now+30000;jobs.save(job);return;}
  boolean retry=false;job.attempts++;
  for(var device:targets){
   String receiptId=job.visitId+":"+device.installationId;
   if(receipts.existsById(receiptId))continue;
   PushSender.Result result;
   try{result=sender.send(device.fcmToken,job.visitId);}catch(RuntimeException e){result=PushSender.Result.RETRY;}
   if(result==PushSender.Result.RETRY){retry=true;continue;}
   var receipt=new PushReceipt();receipt.id=receiptId;receipt.visitId=job.visitId;receipt.installationId=device.installationId;
   receipt.result=result.name();receipt.processedAt=now;receipts.save(receipt);
   if(result==PushSender.Result.INVALID_TOKEN)devices.disableToken(device.installationId,device.fcmToken);
  }
  if(retry){job.lastError="FCM_RETRY";job.nextAttemptAt=now+Math.min(900000L,15000L*(1L<<Math.min(job.attempts,6)));}
  else{job.state="PROCESSED";job.lastError=null;}
  jobs.save(job);
 }
}
