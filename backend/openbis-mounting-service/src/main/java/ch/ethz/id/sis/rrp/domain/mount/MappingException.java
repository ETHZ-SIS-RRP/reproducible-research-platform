package ch.ethz.id.sis.rrp.domain.mount;

public class MappingException extends Exception {
    public MappingException(Throwable cause) {
        super(cause);
    }

    public MappingException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
