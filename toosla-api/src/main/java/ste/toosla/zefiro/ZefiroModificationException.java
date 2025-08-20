package ste.toosla.zefiro;

import java.util.Date;
import java.util.Optional;

/**
 * Exception thrown when a Zefiro operation fails due to a modification conflict.
 * This typically corresponds to an HTTP 412 Precondition Failed status.
 */
public class ZefiroModificationException extends ZefiroException {

    public final Optional<Date> lastModified;

    public ZefiroModificationException() {
        super("Modification conflict detected");
        this.lastModified = Optional.empty();
    }

    public ZefiroModificationException(String message) {
        super(message);
        this.lastModified = Optional.empty();
    }

    public ZefiroModificationException(String message, Throwable cause) {
        super(message, cause);
        this.lastModified = Optional.empty();
    }

    public ZefiroModificationException(Date lastModified) {
        super("Modification conflict detected");
        this.lastModified = Optional.ofNullable(lastModified); // Use Optional.ofNullable to handle null Date input
    }
}