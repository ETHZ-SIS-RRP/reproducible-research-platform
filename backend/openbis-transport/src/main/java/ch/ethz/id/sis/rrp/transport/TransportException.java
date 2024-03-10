package ch.ethz.id.sis.rrp.transport;

public class TransportException extends RuntimeException {
    public TransportException(String reason) {
        super(reason);
    }
}
