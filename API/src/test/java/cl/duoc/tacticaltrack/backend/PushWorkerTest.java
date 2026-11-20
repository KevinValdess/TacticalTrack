package cl.duoc.tacticaltrack.backend;
import org.junit.jupiter.api.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class PushWorkerTest {
 OutboxRepository jobs;ReceiptRepository receipts;DeviceRepository devices;PushSender sender;PushWorker worker;PushOutbox job;SupervisorDevice device;
 @BeforeEach void setup(){
  jobs=mock(OutboxRepository.class);receipts=mock(ReceiptRepository.class);devices=mock(DeviceRepository.class);sender=mock(PushSender.class);
  worker=new PushWorker(jobs,receipts,devices,sender);
  job=new PushOutbox();job.visitId=8L;job.supervisorId="SUP-001";job.createdAt=1000;
  device=new SupervisorDevice();device.installationId="installation-123";device.fcmToken="token-test";
  when(devices.findBySupervisorIdAndActiveTrue("SUP-001")).thenReturn(List.of(device));
 }
 @Test void acceptedIsRecorded(){
  when(sender.send("token-test",8)).thenReturn(PushSender.Result.ACCEPTED);worker.process(job,2000);
  assertEquals("PROCESSED",job.state);verify(receipts).save(argThat(r->r.result.equals("ACCEPTED")&&r.visitId==8L));
 }
 @Test void failedFcmStaysPending(){
  when(sender.send(any(),anyLong())).thenReturn(PushSender.Result.RETRY);worker.process(job,2000);
  assertEquals("PENDING",job.state);assertTrue(job.nextAttemptAt>2000);assertEquals(1,job.attempts);
  verify(receipts,never()).save(any());
 }
 @Test void alreadyAcceptedTargetIsNotResent(){
  when(receipts.existsById("8:installation-123")).thenReturn(true);worker.process(job,2000);
  verifyNoInteractions(sender);assertEquals("PROCESSED",job.state);
 }
 @Test void invalidTokenIsDisabled(){
  when(sender.send(any(),anyLong())).thenReturn(PushSender.Result.INVALID_TOKEN);worker.process(job,2000);
  verify(devices).disableToken(device.installationId,device.fcmToken);verify(receipts).save(argThat(r->r.result.equals("INVALID_TOKEN")));
 }
 @Test void noDeviceWaitsWithoutBurningAttempts(){
  when(devices.findBySupervisorIdAndActiveTrue("SUP-001")).thenReturn(List.of());worker.process(job,2000);
  assertEquals("PENDING",job.state);assertEquals(0,job.attempts);verifyNoInteractions(sender);
 }
 @Test void expiredAndExhaustedJobsDoNotSend(){
  worker.process(job,86402000L);assertEquals("EXPIRED",job.state);
  job.state="PENDING";job.attempts=8;worker.process(job,2000);assertEquals("FAILED",job.state);verifyNoInteractions(sender);
 }
 @Test void runtimeFailureDoesNotEscapeWorker(){
  when(sender.send(any(),anyLong())).thenThrow(new IllegalStateException());worker.process(job,2000);
  assertEquals("PENDING",job.state);assertEquals("FCM_RETRY",job.lastError);
 }
 @Test void pollReadsOnlyDueJobs(){
  when(jobs.findTop20ByStateAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(eq("PENDING"),anyLong())).thenReturn(List.of());
  worker.poll();verifyNoInteractions(sender);
 }
}
