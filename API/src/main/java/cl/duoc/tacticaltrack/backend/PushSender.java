package cl.duoc.tacticaltrack.backend;
public interface PushSender {
 enum Result {ACCEPTED,INVALID_TOKEN,RETRY}
 Result send(String token,long visitId);
}
